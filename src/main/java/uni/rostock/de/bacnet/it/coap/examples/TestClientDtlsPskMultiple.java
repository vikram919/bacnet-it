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
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.constructed.PropertyReference;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.constructed.ReadAccessSpecification;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.constructed.SequenceOf;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.constructed.ServicesSupported;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.util.ByteQueue;
import ch.fhnw.bacnetit.samplesandtests.api.service.acknowledgment.ReadPropertyMultipleAck;
import ch.fhnw.bacnetit.samplesandtests.api.service.confirmed.ReadPropertyMultipleRequest;
import ch.qos.logback.classic.Logger;
import uni.rostock.de.bacnet.it.coap.transportbinding.TransportDTLSCoapBinding;

public class TestClientDtlsPskMultiple {

	private static Logger LOG = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

	private static final int DTLS_PORT = 5685;
	private static final int DEVICE_ID = 120;
	private static final int AUTH_ID = 1;
	private static String AUTH_IP;
	private int interMessageId = 0;
	private boolean signal = false;
	double requestSendingTime = 0;

	public static void main(String[] args) {
		AUTH_IP = args[0];
		TestClientDtlsPskMultiple testClient = new TestClientDtlsPskMultiple();
		

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

		channelConfigure.registerChannelListener(new ChannelListener(new BACnetEID(DEVICE_ID)) {

			@Override
			public void onIndication(T_UnitDataIndication arg0, Object arg1) {

				LOG.debug("message T_unitDataIndication");
				ASDU receivedRequest = testClient.getServiceFromBody(arg0.getData().getBody());
				if (receivedRequest == null) {
					System.out.println("received request is null bro!");
				}
				// Dummy Handling of a ReadPropertyAck
				if (receivedRequest instanceof ComplexACK) {
					if (((ComplexACK) receivedRequest).getService().getChoiceId() == 14) {
						ReadPropertyMultipleAck readPropertyMultipleAck = (ReadPropertyMultipleAck) ((ComplexACK) receivedRequest)
								.getService();
						double currentTime = System.nanoTime();
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

	public double getTimeStamp() {
		return this.requestSendingTime;
	}

	public synchronized void waitForSignal() {
		if (!signal) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			signal = false;
		}
	}

	public synchronized void signal() {
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

	public void sendReadPropertyMultipleRequest(URI destination, BACnetEID from, BACnetEID to, ASEServices aseService)
			throws URISyntaxException {
		final ByteQueue byteQueue = new ByteQueue();
		SequenceOf<ReadAccessSpecification> specs = new SequenceOf<>();
		SequenceOf<PropertyReference> listOfPropertyReferences = new SequenceOf<>();
		listOfPropertyReferences.add(new PropertyReference(BACnetPropertyIdentifier.presentValue));
		specs.add(new ReadAccessSpecification(new BACnetObjectIdentifier(BACnetObjectType.analogValue, 1),
				listOfPropertyReferences));
		new ReadPropertyMultipleRequest(specs).write(byteQueue);
		final TPDU tpdu = new TPDU(from, to, byteQueue.peekAll());
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
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			try {
				for (int j = 0; j < 20; j++) {
					setTimeStamp(System.nanoTime());
					sendReadPropertyMultipleRequest(new URI("coaps://"+AUTH_IP+":5684"), new BACnetEID(DEVICE_ID),
							new BACnetEID(AUTH_ID), aseService);
					waitForSignal();
				}
			} catch (Exception e) {
				System.err.print(e);
			}
		}

	}

}
