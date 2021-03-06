package uni.rostock.de.bacnet.it.coap.examples;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;

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
import ch.fhnw.bacnetit.transportbinding.api.BindingConfiguration;
import ch.fhnw.bacnetit.transportbinding.api.ConnectionFactory;
import ch.fhnw.bacnetit.transportbinding.api.TransportBindingInitializer;
import ch.fhnw.bacnetit.transportbinding.ws.incoming.api.WSConnectionServerFactory;
import ch.fhnw.bacnetit.transportbinding.ws.outgoing.api.WSConnectionClientFactory;

public class TestServer {

	private static Logger LOG = (Logger) LoggerFactory.getLogger(TestServer.class);

	private static final int WS_PORT = 1080;
	private ASEServices aseService;
	private static final int DEVICE_ID = 120;
	private static final int AUTH_ID = 1;

	public static void main(String[] args) {

		TestServer testServer = new TestServer();
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
		testServer.aseService = ChannelFactory.getInstance();

		/* Add transport binding to ASEService by using ChannelConfiguration */
		ChannelConfiguration channelConfigure = testServer.aseService;
		final ConnectionFactory connectionFactory = new ConnectionFactory();
		connectionFactory.addConnectionClient("ws", new WSConnectionClientFactory());
		connectionFactory.addConnectionServer("ws", new WSConnectionServerFactory(WS_PORT));

		BindingConfiguration bindingConfiguration = new TransportBindingInitializer();
		bindingConfiguration.initializeAndStart(connectionFactory);
		channelConfigure.setASEService((ASEService) bindingConfiguration);

		channelConfigure.setEntityListener(new BACnetEntityListener() {

			@Override
			public void onRemoteRemove(BACnetEID eid) {
			}

			@Override
			public void onRemoteAdded(BACnetEID eid, URI uri) {
				DirectoryService.getInstance().register(eid, uri, false, true);
			}

			@Override
			public void onLocalRequested(BACnetEID eid) {
			}
		});

		channelConfigure.registerChannelListener(new ChannelListener(new BACnetEID(AUTH_ID)) {

			private Scanner scanner;

			@Override
			public void onIndication(T_UnitDataIndication arg0, Object arg1) {

				LOG.debug("message T_unitDataIndication");
				System.out.println("request received!!");
				ASDU receivedRequest = testServer.getServiceFromBody(arg0.getData().getBody());
				if (receivedRequest instanceof ConfirmedRequest
						&& ((ConfirmedRequest) receivedRequest).getServiceRequest() instanceof ReadPropertyRequest) {
					System.out.println("confirmed request received!!");
					// Prepare DUMMY answer
					final ByteQueue byteQueue = new ByteQueue();
					new ReadPropertyAck(new BACnetObjectIdentifier(BACnetObjectType.analogValue, 1),
							BACnetPropertyIdentifier.presentValue, new UnsignedInteger(1), new Real(System.nanoTime()))
									.write(byteQueue);
					String hostAddress = null;
					if (arg0.getSourceAddress() != null) {
						hostAddress = ((InetSocketAddress) arg0.getSourceAddress()).getHostString();
					} else {
						throw new NullPointerException("host address of message cannot be null");
					}
					LOG.debug("TPDU defined");
					try {
						testServer.sendBACnetMessage(new URI("ws://" + hostAddress + ":9090"), new BACnetEID(AUTH_ID),
								new BACnetEID(DEVICE_ID), byteQueue.popAll(), testServer.aseService);
					} catch (URISyntaxException e) {
						e.printStackTrace();
					}
					System.out.println("respone sent!!");
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

	public void sendBACnetMessage(URI destination, BACnetEID from, BACnetEID to, byte[] confirmedBacnetMessage, ASEServices aseService)
			throws URISyntaxException {

		final TPDU tpdu = new TPDU(from, to, confirmedBacnetMessage);

		final T_UnitDataRequest unitDataRequest = new T_UnitDataRequest(destination, tpdu, 1, null);

		aseService.doRequest(unitDataRequest);
	}
}
