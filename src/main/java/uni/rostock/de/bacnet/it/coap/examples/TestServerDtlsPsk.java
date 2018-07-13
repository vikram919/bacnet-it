package uni.rostock.de.bacnet.it.coap.examples;

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.fhnw.bacnetit.ase.application.configuration.api.DiscoveryConfig;
import ch.fhnw.bacnetit.ase.application.service.api.ASEServices;
import ch.fhnw.bacnetit.ase.application.service.api.BACnetEntityListener;
import ch.fhnw.bacnetit.ase.application.service.api.ChannelConfiguration;
import ch.fhnw.bacnetit.ase.application.service.api.ChannelFactory;
import ch.fhnw.bacnetit.ase.application.transaction.api.ChannelListener;
import ch.fhnw.bacnetit.ase.encoding.api.BACnetEID;
import ch.fhnw.bacnetit.ase.encoding.api.TPDU;
import ch.fhnw.bacnetit.ase.encoding.api.T_ReportIndication;
import ch.fhnw.bacnetit.ase.encoding.api.T_UnitDataIndication;
import ch.fhnw.bacnetit.ase.encoding.api.T_UnitDataRequest;
import ch.fhnw.bacnetit.ase.network.directory.api.DirectoryService;
import ch.fhnw.bacnetit.ase.transportbinding.service.api.ASEService;
import ch.fhnw.bacnetit.directorybinding.dnssd.api.DNSSD;
import ch.fhnw.bacnetit.samplesandtests.api.deviceobjects.BACnetObjectIdentifier;
import ch.fhnw.bacnetit.samplesandtests.api.deviceobjects.BACnetObjectType;
import ch.fhnw.bacnetit.samplesandtests.api.deviceobjects.BACnetPropertyIdentifier;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.asdu.ASDU;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.asdu.ConfirmedRequest;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.asdu.IncomingRequestParser;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.constructed.ServicesSupported;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.primitive.Real;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.primitive.UnsignedInteger;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.util.ByteQueue;
import ch.fhnw.bacnetit.samplesandtests.api.service.acknowledgment.ReadPropertyAck;
import ch.fhnw.bacnetit.samplesandtests.api.service.confirmed.ReadPropertyRequest;
import uni.rostock.de.bacnet.it.coap.transportbinding.ResponseCallback;
import uni.rostock.de.bacnet.it.coap.transportbinding.TransportDTLSCoapBinding;

public class TestServerDtlsPsk {

	private static Logger LOG = (Logger) LoggerFactory.getLogger(TestServer.class);
	
	private static final int DTLS_PORT = 5684;
	private ASEServices aseService;
	private static final int DEVICE_ID = 120;
	private static final int AUTH_ID = 1;

	public static void main(String[] args) {

		TestServerDtlsPsk testServer = new TestServerDtlsPsk();
		/**
		 * Initialise directory service but not used in this example
		 */
		final DiscoveryConfig ds = new DiscoveryConfig("DNSSD", "1.1.1.1", "itb.bacnet.ch.", "bds._sub._bacnet._tcp.",
				"dev._sub._bacnet._tcp.", "obj._sub._bacnet._tcp.", false);

		try {
			DirectoryService.init();
			DirectoryService.getInstance().setDNSBinding(new DNSSD(ds));

		} catch (final Exception e1) {
			e1.printStackTrace();
		}
		testServer.start();
	}

	public void start() {

		aseService = ChannelFactory.getInstance();
		/* Add transport binding to ASEService by using ChannelConfiguration */
		ChannelConfiguration channelConfigure = aseService;
		TransportDTLSCoapBinding transportDtlsCoapBinding = new TransportDTLSCoapBinding();
		transportDtlsCoapBinding.setPskMode();
		transportDtlsCoapBinding.createSecureCoapServer(DTLS_PORT);
		transportDtlsCoapBinding.createSecureCoapClient();
		transportDtlsCoapBinding.init();
		channelConfigure.setASEService((ASEService) transportDtlsCoapBinding);

		channelConfigure.setEntityListener(new BACnetEntityListener() {

			@Override
			public void onRemoteRemove(BACnetEID eid) {

			}

			@Override
			public void onRemoteAdded(BACnetEID eid, URI uri) {

			}

			@Override
			public void onLocalRequested(BACnetEID eid) {

			}
		});

		channelConfigure.registerChannelListener(new ChannelListener(new BACnetEID(AUTH_ID)) {

			@Override
			public void onIndication(T_UnitDataIndication arg0, Object arg1) {

				LOG.debug("message T_unitDataIndication");
				ASDU receivedRequest = getServiceFromBody(arg0.getData().getBody());
				if (receivedRequest instanceof ConfirmedRequest
						&& ((ConfirmedRequest) receivedRequest).getServiceRequest() instanceof ReadPropertyRequest) {

					// Prepare DUMMY answer
					final ByteQueue byteQueue = new ByteQueue();
					new ReadPropertyAck(new BACnetObjectIdentifier(BACnetObjectType.analogValue, 1),
							BACnetPropertyIdentifier.presentValue, new UnsignedInteger(1), new Real(System.nanoTime()))
									.write(byteQueue);

					if (arg0.getDataExpectingReply()) {
						ResponseCallback responseCallback = (ResponseCallback) arg1;
						TPDU tpdu = new TPDU(new BACnetEID(AUTH_ID), new BACnetEID(DEVICE_ID), byteQueue.popAll());
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

	protected ASDU getServiceFromBody(final byte[] body) {
		final ByteQueue queue = new ByteQueue(body);
		final ServicesSupported servicesSupported = new ServicesSupported();
		servicesSupported.setAll(true);
		final IncomingRequestParser parser = new IncomingRequestParser(servicesSupported, queue);
		ASDU request = null;

		try {
			request = parser.parse();
		} catch (final Exception e) {
			System.out.println(e);
		}
		return request;
	}

	public void sendBACnetMessage(URI destination, BACnetEID from, BACnetEID to, byte[] confirmedBacnetMessage)
			throws URISyntaxException {

		final TPDU tpdu = new TPDU(from, to, confirmedBacnetMessage);
		final T_UnitDataRequest unitDataRequest = new T_UnitDataRequest(destination, tpdu, 1, null);

		aseService.doRequest(unitDataRequest);
	}
}
