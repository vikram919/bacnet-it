package uni.rostock.de.bacnet.it.coap.examples;

import java.net.URI;
import java.net.URISyntaxException;

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
import ch.fhnw.bacnetit.samplesandtests.api.encoding.asdu.ComplexACK;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.asdu.IncomingRequestParser;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.constructed.ServicesSupported;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.util.ByteQueue;
import ch.fhnw.bacnetit.samplesandtests.api.service.acknowledgment.ReadPropertyAck;
import ch.fhnw.bacnetit.samplesandtests.api.service.confirmed.ReadPropertyRequest;
import ch.fhnw.bacnetit.transportbinding.api.BindingConfiguration;
import ch.fhnw.bacnetit.transportbinding.api.ConnectionFactory;
import ch.fhnw.bacnetit.transportbinding.api.TransportBindingInitializer;
import ch.fhnw.bacnetit.transportbinding.ws.incoming.api.WSConnectionServerFactory;
import ch.fhnw.bacnetit.transportbinding.ws.outgoing.api.WSConnectionClientFactory;
import ch.qos.logback.classic.Logger;

public class TestClient {

	private static Logger LOG = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

	private static final int WS_PORT = 9090;
	private static final int DEVICE_ID = 120;
	private static final int AUTH_ID = 1;
	private int interMessageId = 0;
	private boolean signal = false;
	double requestSendingTime = 0;

	public static void main(String[] args) {

		TestClient testClient = new TestClient();

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

		ASEServices aseService = ChannelFactory.getInstance();
		/* Add transport binding to ASEService by using ChannelConfiguration */
		ChannelConfiguration channelConfigure = aseService;
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
			}

			@Override
			public void onLocalRequested(BACnetEID eid) {

			}
		});

		channelConfigure.registerChannelListener(new ChannelListener(new BACnetEID(DEVICE_ID)) {

			@Override
			public void onIndication(T_UnitDataIndication arg0, Object arg1) {

				LOG.debug("message T_unitDataIndication");
				System.out.println("response received!");
				ASDU receivedRequest = testClient.getServiceFromBody(arg0.getData().getBody());
				// Dummy Handling of a ReadPropertyAck
				if (receivedRequest instanceof ComplexACK) {
					if (((ComplexACK) receivedRequest).getService().getChoiceId() == 12) {
						ReadPropertyAck readPropertyAck = (ReadPropertyAck) ((ComplexACK) receivedRequest).getService();
						double currentTime = System.nanoTime();
						String stringVal = String.valueOf(readPropertyAck.getValue());
						float receivedTime = Float.parseFloat(stringVal);
						int delay = (int) ((currentTime - testClient.getTimeStamp()) / 1000000);
						testClient.setMessageId(testClient.getInternalMessageId() + 1);
						System.out.println(delay);
						testClient.signal();
					}
				}
			}

			@Override
			public void onError(T_ReportIndication tReportIndication, String cause) {
				LOG.error("TransportError to destination: {} because of {}",
						new Object[] { tReportIndication.getDestinationAddress(), cause });
			}
		});

		Thread requestThread = new Thread(testClient.new RequestThread(aseService));
		requestThread.start();

	}

	public void start() {

	}

	public double getTimeStamp() {
		return this.requestSendingTime;
	}

	public void waitForSignal() {
		if (!signal) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			signal = false;
		}
	}

	public void signal() {
		notify();
		signal = true;
	}

	public int getInternalMessageId() {
		return this.interMessageId;
	}

	public void setMessageId(int value) {
		this.interMessageId = value;
	}

	public void setTimeStamp(double value) {
		this.requestSendingTime = value;
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

	public void sendReadPropertyRequest(URI destination, BACnetEID from, BACnetEID to, ASEServices aseService)
			throws URISyntaxException {
		final ReadPropertyRequest readRequest = new ReadPropertyRequest(
				new BACnetObjectIdentifier(BACnetObjectType.analogValue, 1), BACnetPropertyIdentifier.presentValue);
		final ByteQueue byteQueue = new ByteQueue();
		readRequest.write(byteQueue);
		final TPDU tpdu = new TPDU(from, to, byteQueue.popAll());

		final T_UnitDataRequest unitDataRequest = new T_UnitDataRequest(destination, tpdu, 1, null);
		if (aseService != null) {
			aseService.doRequest(unitDataRequest);
		} else {
			System.out.println("null aseService");
		}
	}

	class RequestThread implements Runnable {
		ASEServices aseService;

		public RequestThread(ASEServices aseService) {
			this.aseService = aseService;
		}

		@Override
		public void run() {
			
			try {
				aseService.connect(new URI("ws://127.0.0.1:1080"));
				Thread.sleep(3000);
				for (int j = 0; j < 5000; j++) {
					setTimeStamp(System.nanoTime());
					sendReadPropertyRequest(new URI("ws://127.0.0.1:1080"), new BACnetEID(DEVICE_ID),
							new BACnetEID(AUTH_ID), aseService);
					waitForSignal();
				}
			} catch (Exception e) {
				System.err.print(e);
			}
		}

	}

}
