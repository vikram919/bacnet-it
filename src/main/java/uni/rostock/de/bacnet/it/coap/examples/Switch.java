package uni.rostock.de.bacnet.it.coap.examples;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.fhnw.bacnetit.ase.application.configuration.api.DiscoveryConfig;
import ch.fhnw.bacnetit.ase.application.service.api.ASEServices;
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
import ch.fhnw.bacnetit.samplesandtests.api.encoding.exception.BACnetException;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.constructed.ServicesSupported;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.primitive.OctetString;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.primitive.UnsignedInteger;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.util.ByteQueue;
import ch.fhnw.bacnetit.samplesandtests.api.service.confirmed.WritePropertyRequest;
import uni.rostock.de.bacnet.it.coap.oobAuth.OobAuthClient;
import uni.rostock.de.bacnet.it.coap.transportbinding.TransportDTLSCoapBinding;

public class Switch {

	private static final Logger LOG = LoggerFactory.getLogger(Switch.class);

	private TransportDTLSCoapBinding bindingConfiguration = new TransportDTLSCoapBinding();
	ASEServices aseServiceChannel;
	private static final int AUTH_ID = 1;
//	private static final String AUTH_IP = "139.30.202.56:";
//	private static final String SECURE_SCHEME = "coaps://";
	private static final int DTLS_PORT = 5685;
	private static final BACnetEID AUTH_EID = new BACnetEID(AUTH_ID);
	private OobAuthClient oobClient;

	/* we assume OOB password is known to both */
	private static String OOB_PSWD_STRING = "10101110010101101011";

	public static void main(String[] args) {

		Switch device = new Switch();
		final DiscoveryConfig ds = new DiscoveryConfig("DNSSD", "1.1.1.1", "itb.bacnet.ch.", "bds._sub._bacnet._tcp.",
				"dev._sub._bacnet._tcp.", "obj._sub._bacnet._tcp.", false);

		try {
			DirectoryService.init();
			DirectoryService.getInstance().setDNSBinding(new DNSSD(ds));

		} catch (final Exception e1) {
			e1.printStackTrace();
		}
//		device.hostAddress();
//		PushButtonJob pushButtonJob = new PushButtonJob();
//		pushButtonJob.start();
		// pushButtonJob.getOOBKeyAsString();
		device.oobClient = new OobAuthClient(OOB_PSWD_STRING, "coap://localhost:5683/authentication",
				device.bindingConfiguration);
		device.oobClient.startHandShake();
		device.start();
		try {
			device.sendWritePropertRequest(new URI("coaps://localhost:5684"));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	public void start() {

		aseServiceChannel = ChannelFactory.getInstance();
		ChannelConfiguration channelConfigure = aseServiceChannel;
		bindingConfiguration.setPskMode();
		bindingConfiguration.createSecureCoapClient();
		bindingConfiguration.createSecureCoapServer(DTLS_PORT);
		bindingConfiguration.init();
		channelConfigure.setASEService((ASEService) bindingConfiguration);

		channelConfigure.registerChannelListener(new ChannelListener(new BACnetEID(oobClient.getDeviceId())) {

			@Override
			public void onIndication(T_UnitDataIndication arg0, Object arg1) {
				LOG.debug("message T_unitDataIndication");
				ASDU receivedRequest = getServiceFromBody(arg0.getData().getBody());
				if (receivedRequest instanceof ConfirmedRequest
						&& ((ConfirmedRequest) receivedRequest).getServiceRequest() instanceof WritePropertyRequest) {
					LOG.debug("device received a WritePropertyRequest!");
					// FIXME: dirtyhack, get propertyvalue using wrightproperty
					ByteQueue queue = new ByteQueue(arg0.getData().getBody());
					byte[] msg = queue.peek(15, queue.size() - 21);
					LOG.info("Message from authorizer: " + new String(msg));
				} else {
					LOG.debug("Device received a ack message");
				}
			}

			@Override
			public void onError(T_ReportIndication tReportIndication, String cause) {

			}
		});
	}

	public ASDU getServiceFromBody(byte[] body) {
		ASDU request = null;
		try {
			ByteQueue queue = new ByteQueue(body);
			ServicesSupported servicesSupported = new ServicesSupported();
			servicesSupported.setAll(true);
			IncomingRequestParser requestParser = new IncomingRequestParser(servicesSupported, queue);
			request = requestParser.parse();
		} catch (BACnetException e) {
			e.printStackTrace();
		}
		return request;
	}

	public void sendWritePropertRequest(URI uri) {
		try {
			final WritePropertyRequest readRequest = new WritePropertyRequest(
					new BACnetObjectIdentifier(BACnetObjectType.analogValue, 1), BACnetPropertyIdentifier.presentValue,
					new UnsignedInteger(55), new OctetString("hello server".getBytes(StandardCharsets.UTF_8)),
					new UnsignedInteger(1));
			final ByteQueue byteQueue = new ByteQueue();
			readRequest.write(byteQueue);
			byte[] body = byteQueue.popAll();
			ByteQueue testQueue = new ByteQueue();
			readRequest.getPropertyValue().write(testQueue);
			final TPDU tpdu = new TPDU(new BACnetEID(oobClient.getDeviceId()), AUTH_EID, body);
			final T_UnitDataRequest unitDataRequest = new T_UnitDataRequest(uri, tpdu, 1, null);
			aseServiceChannel.doRequest(unitDataRequest);
			LOG.debug("WritePropertyRequest sent");

		} catch (Exception e) {
			System.err.print(e);
		}
	}
}
