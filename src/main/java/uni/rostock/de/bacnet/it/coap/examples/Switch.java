package uni.rostock.de.bacnet.it.coap.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.fhnw.bacnetit.ase.application.configuration.api.DiscoveryConfig;
import ch.fhnw.bacnetit.ase.application.service.api.ASEServices;
import ch.fhnw.bacnetit.ase.application.service.api.ChannelConfiguration;
import ch.fhnw.bacnetit.ase.application.service.api.ChannelFactory;
import ch.fhnw.bacnetit.ase.application.transaction.api.ChannelListener;
import ch.fhnw.bacnetit.ase.encoding.api.BACnetEID;
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
import uni.rostock.de.bacnet.it.coap.oobAuth.ApplicationMessages;
import uni.rostock.de.bacnet.it.coap.oobAuth.OobAuthClient;
import uni.rostock.de.bacnet.it.coap.transportbinding.TransportDTLSCoapBinding;

public class Switch {

	private static final Logger LOG = LoggerFactory.getLogger(Switch.class);

	private TransportDTLSCoapBinding bindingConfiguration = new TransportDTLSCoapBinding();
	ASEServices aseServiceChannel;
	private static final int AUTH_ID = 1;
	private static final String OOB_PASSWORD_STRING = "01001110100010111001";
	 private static final String AUTH_IP = "139.30.202.56:";
	// private static final String SECURE_SCHEME = "coaps://";
	private static final int DTLS_PORT = 5684;
	private static final BACnetEID AUTH_EID = new BACnetEID(AUTH_ID);

	private OobAuthClient oobAuthClient;

	public static void main(String[] args) {

		Switch device = new Switch();
		LedFlash pushButtonJob = new LedFlash();
		pushButtonJob.start();
		String oobPswdBitString = pushButtonJob.getOOBKeyAsString();
		device.oobAuthClient = new OobAuthClient(oobPswdBitString, "coap://"+AUTH_IP+"5683/authentication",
				device.bindingConfiguration);
		device.oobAuthClient.startHandShake();
		try {
			final DiscoveryConfig ds = new DiscoveryConfig("DNSSD", "1.1.1.1", "itb.bacnet.ch.",
					"bds._sub._bacnet._tcp.", "dev._sub._bacnet._tcp.", "obj._sub._bacnet._tcp.", false);
			DirectoryService.init();
			DirectoryService.getInstance().setDNSBinding(new DNSSD(ds));

		} catch (final Exception e1) {
			e1.printStackTrace();
		}
		//device.start();
//		byte[] message = "Hello Server".getBytes(StandardCharsets.UTF_8);
//		ApplicationMessages.sendWritePropertyRequest(device.aseServiceChannel, message,
//				new BACnetEID(device.oobAuthClient.getDeviceId()), AUTH_EID, "coaps://"+AUTH_IP+"5684");
	}

	public void start() {

		aseServiceChannel = ChannelFactory.getInstance();
		ChannelConfiguration channelConfigure = aseServiceChannel;
		bindingConfiguration.setPskMode();
		bindingConfiguration.createSecureCoapClient();
		bindingConfiguration.createSecureCoapServer(DTLS_PORT);
		bindingConfiguration.init();
		channelConfigure.setASEService((ASEService) bindingConfiguration);

		channelConfigure.registerChannelListener(new ChannelListener(new BACnetEID(oobAuthClient.getDeviceId())) {

			@Override
			public void onIndication(T_UnitDataIndication arg0, Object arg1) {
				LOG.debug("message T_unitDataIndication");
				ASDU incoming = ApplicationMessages.getServiceFromBody(arg0.getData().getBody());
				if (incoming instanceof ConfirmedRequest
						&& ((ConfirmedRequest) incoming).getServiceRequest() instanceof WritePropertyRequest) {
					LOG.debug("device received a WritePropertyRequest!");
					// FIXME: dirtyhack, get propertyvalue using wrightproperty
					ByteQueue queue = new ByteQueue(arg0.getData().getBody());
					byte[] msg = queue.peek(15, queue.size() - 21);
					LOG.info("Message from authorizer: " + new String(msg));
				}
				if (incoming instanceof SimpleACK) {
					LOG.debug("Device received a ack message");
				}
			}

			@Override
			public void onError(T_ReportIndication tReportIndication, String cause) {

			}
		});
	}
}
