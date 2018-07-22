package uni.rostock.de.bacnet.it.coap.examples;

import org.eclipse.californium.core.coap.CoAP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.fhnw.bacnetit.ase.application.configuration.api.DiscoveryConfig;
import ch.fhnw.bacnetit.ase.application.service.api.ASEServices;
import ch.fhnw.bacnetit.ase.application.service.api.ApplicationService;
import ch.fhnw.bacnetit.ase.application.service.api.ChannelConfiguration;
import ch.fhnw.bacnetit.ase.application.service.api.ChannelFactory;
import ch.fhnw.bacnetit.ase.application.transaction.api.ChannelListener;
import ch.fhnw.bacnetit.ase.encoding.api.BACnetEID;
import ch.fhnw.bacnetit.ase.encoding.api.TPDU;
import ch.fhnw.bacnetit.ase.encoding.api.T_ReportIndication;
import ch.fhnw.bacnetit.ase.encoding.api.T_UnitDataIndication;
import ch.fhnw.bacnetit.ase.network.directory.api.DirectoryService;
import ch.fhnw.bacnetit.ase.transportbinding.service.api.ASEService;
import ch.fhnw.bacnetit.directorybinding.dnssd.api.DNSSD;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.asdu.ASDU;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.asdu.ConfirmedRequest;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.asdu.SimpleACK;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.util.ByteQueue;
import ch.fhnw.bacnetit.samplesandtests.api.service.confirmed.WritePropertyRequest;
import uni.rostock.de.bacnet.it.coap.oobAuth.AddDeviceRequest;
import uni.rostock.de.bacnet.it.coap.oobAuth.ApplicationMessages;
import uni.rostock.de.bacnet.it.coap.oobAuth.OobAuthServer;
import uni.rostock.de.bacnet.it.coap.oobAuth.OobProtocol;
import uni.rostock.de.bacnet.it.coap.oobAuth.OobSessionsStore;
import uni.rostock.de.bacnet.it.coap.transportbinding.ResponseCallback;
import uni.rostock.de.bacnet.it.coap.transportbinding.TransportDTLSCoapBinding;

public class Authorizer {

	private static final Logger LOG = LoggerFactory.getLogger(Authorizer.class);
	private static final int AUTH_ID = 1;
	private ASEServices aseServices;
	private ApplicationService aseService;
	private TransportDTLSCoapBinding coapDtlsbindingConfig;
	private static String OOB_PSWD_STRING = "10101110010101101011";
	private OobSessionsStore deviceSessionsMap = OobSessionsStore.getInstance();

	public static void main(String[] args) {

		Authorizer authorizer = new Authorizer();
		authorizer.start();

		OobAuthServer oobAuthServer = new OobAuthServer(authorizer.coapDtlsbindingConfig, authorizer.deviceSessionsMap);
		oobAuthServer.startAuthServer(CoAP.DEFAULT_COAP_PORT);
		authorizer.deviceSessionsMap.addDeviceoobPswd(OOB_PSWD_STRING);

		try {
			DiscoveryConfig ds = new DiscoveryConfig("DNSSD", "1.1.1.1", "itb.bacnet.ch.", "bds._sub._bacnet._udp.",
					"auth._sub._bacnet._udp.", "authservice._sub._bacnet._udp.", false);
			DirectoryService.init();
			DirectoryService.getInstance().setDNSBinding(new DNSSD(ds));
		} catch (final Exception e1) {
			e1.printStackTrace();
		}
	}

	public void start() {

		aseServices = ChannelFactory.getInstance();
		aseService = aseServices;
		/* Add transport binding to ASEService by using ChannelConfiguration */
		ChannelConfiguration channelConfigure = (ChannelConfiguration) aseService;
		// configure transport binding to coap dtls
		coapDtlsbindingConfig = new TransportDTLSCoapBinding();
		coapDtlsbindingConfig.setAllModes();
		coapDtlsbindingConfig.createSecureCoapClient();
		coapDtlsbindingConfig.createSecureCoapServer(CoAP.DEFAULT_COAP_SECURE_PORT);

		coapDtlsbindingConfig.init();

		channelConfigure.setASEService((ASEService) coapDtlsbindingConfig);

		channelConfigure.registerChannelListener(new ChannelListener(new BACnetEID(AUTH_ID)) {

			@Override
			public void onIndication(T_UnitDataIndication arg0, Object arg1) {

				LOG.debug("message T_unitDataIndication");
				ASDU receivedRequest = ApplicationMessages.getServiceFromBody(arg0.getData().getBody());
				if (receivedRequest instanceof ConfirmedRequest
						&& ((ConfirmedRequest) receivedRequest).getServiceRequest() instanceof WritePropertyRequest) {
					LOG.debug("authorizer received a WritePropertyRequest!");
					ByteQueue queue = new ByteQueue(arg0.getData().getBody());
					byte[] msg = queue.peek(15, queue.size() - 21);
					if (msg[0] >> 5 == OobProtocol.ADD_DEVICE_REQUEST) {
						LOG.info("authorizer received AddDeviceRequest from mobile");
						AddDeviceRequest addDeviceRequest = new AddDeviceRequest(msg);
						/*
						 * Authorizer adds the received oob password key from AddDeviceRequest to its
						 * DeviceSessionstore
						 */
						deviceSessionsMap.addDeviceoobPswd(addDeviceRequest.getBitKeyString());
					}
					if (arg0.getDataExpectingReply()) {
						final int serviceAckChoice = ((ConfirmedRequest) receivedRequest).getServiceRequest()
								.getChoiceId();
						ByteQueue byteQueue = new ByteQueue();
						new SimpleACK(serviceAckChoice).write(byteQueue);
						ResponseCallback responseCallback = (ResponseCallback) arg1;
						TPDU tpdu = new TPDU(new BACnetEID(AUTH_ID),
								new BACnetEID(arg0.getData().getSourceEID().getIdentifier()), byteQueue.popAll());
						responseCallback.sendResponse(tpdu);
					}
				}
			}

			@Override
			public void onError(T_ReportIndication tReportIndication, String cause) {
				LOG.error("TransportError to destination: {} because of {}",
						new Object[] { tReportIndication.getDestinationAddress(), cause });
			}
		});
	}
}
