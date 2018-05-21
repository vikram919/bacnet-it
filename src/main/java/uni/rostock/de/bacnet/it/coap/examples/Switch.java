package uni.rostock.de.bacnet.it.coap.examples;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.scandium.util.ByteArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.fhnw.bacnetit.ase.application.service.api.ASEServices;
import ch.fhnw.bacnetit.ase.application.service.api.ChannelConfiguration;
import ch.fhnw.bacnetit.ase.application.service.api.ChannelFactory;
import ch.fhnw.bacnetit.ase.application.transaction.api.ChannelListener;
import ch.fhnw.bacnetit.ase.encoding.api.BACnetEID;
import ch.fhnw.bacnetit.ase.encoding.api.TPDU;
import ch.fhnw.bacnetit.ase.encoding.api.T_ReportIndication;
import ch.fhnw.bacnetit.ase.encoding.api.T_UnitDataIndication;
import ch.fhnw.bacnetit.ase.encoding.api.T_UnitDataRequest;
import ch.fhnw.bacnetit.ase.transportbinding.service.api.ASEService;
import ch.fhnw.bacnetit.samplesandtests.api.deviceobjects.BACnetObjectIdentifier;
import ch.fhnw.bacnetit.samplesandtests.api.deviceobjects.BACnetObjectType;
import ch.fhnw.bacnetit.samplesandtests.api.deviceobjects.BACnetPropertyIdentifier;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.asdu.ASDU;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.asdu.ConfirmedRequest;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.asdu.IncomingRequestParser;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.exception.BACnetException;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.constructed.SequenceOf;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.constructed.ServicesSupported;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.primitive.CharacterString;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.primitive.Real;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.primitive.UnsignedInteger;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.util.ByteQueue;
import ch.fhnw.bacnetit.samplesandtests.api.service.confirmed.AddListElementRequest;
import ch.fhnw.bacnetit.samplesandtests.api.service.confirmed.WritePropertyRequest;
import uni.rostock.de.bacnet.it.coap.crypto.EcdhHelper;
import uni.rostock.de.bacnet.it.coap.messageType.Dh1Message;
import uni.rostock.de.bacnet.it.coap.messageType.Dh2Message;
import uni.rostock.de.bacnet.it.coap.messageType.OOBProtocol;
import uni.rostock.de.bacnet.it.coap.messageType.OobFinalMessage;
import uni.rostock.de.bacnet.it.coap.transportbinding.TransportDTLSCoapBinding;

public class Switch {

	private static final Logger LOG = LoggerFactory.getLogger(Switch.class);

	private TransportDTLSCoapBinding bindingConfiguration = new TransportDTLSCoapBinding();
	ASEServices aseServiceChannel;
	private int deviceId;
	private static final int AUTH_ID = 1;
	private static final String AUTH_IP = "139.30.202.56:";
	private String hostAddress;
	private static final String PLAIN_SCHEME = "coap://";
	private static final String SECURE_SCHEME = "coaps://";
	private static final String AUTH_URL = PLAIN_SCHEME + AUTH_IP + "5683/auth";
	private static final int DTLS_PORT = 5684;
	private static final BACnetEID AUTH_EID = new BACnetEID(AUTH_ID);

	/* we assume OOB password is known to both */
	private String oobPswdKey = "10101111010100101010";
	EcdhHelper ecdhHelper;
	private boolean signal = false;

	private CoapClient client;

	public static void main(String[] args) {

		Switch device = new Switch();
		device.hostAddress();
		PushButtonJob pushButtonJob = new PushButtonJob();
		pushButtonJob.start();
		device.ecdhHelper = new EcdhHelper(OOBProtocol.X25519.getValue(), pushButtonJob.getOOBKeyAsString());
		/* creates a plain coap client for initial DH authenticaiton stage */
		device.createCoapClient();
		/* device sends Dh1Message to Authorizer */
		Dh1Message dh1Message = new Dh1Message(device.ecdhHelper);
		device.sendMessage(dh1Message.getBA(), AUTH_URL);
		LOG.info("device sent Dh1Messag to authorizer with publickey: {}",
				ByteArrayUtils.toHex(device.ecdhHelper.getPubKeyBytes()));
		LOG.info("device is waiting for Dh2Message from authorizer");
		device.waitForSignal();
		LOG.info("Dh2Message message received, waiting finished");
		LOG.info("derived shared secret on device side: {}", ByteArrayUtils.toHex(device.ecdhHelper.getSharedSecret()));
		/* device sends final message to authorizer */
		OobFinalMessage oobFinalMessage = new OobFinalMessage(device.getDeviceId(), AUTH_ID, device.ecdhHelper);
		device.sendMessage(oobFinalMessage.getBA(), AUTH_URL);
		LOG.info("final message sent from device");
		LOG.info("handshake successfull on device side");
		try {
			device.bindingConfiguration.addPSK(new String(device.ecdhHelper.getOObPswdKeyIdentifier()),
					device.ecdhHelper.getSharedSecret(), new InetSocketAddress(InetAddress.getByName(AUTH_IP), 5684));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		device.start();
		device.sendBacnetRegisterMessage();
	}

	public void start() {

		aseServiceChannel = ChannelFactory.getInstance();
		ChannelConfiguration channelConfigure = aseServiceChannel;

		bindingConfiguration.createSecureCoapClient();
		bindingConfiguration.createSecureCoapServer(DTLS_PORT);
		bindingConfiguration.init();
		channelConfigure.setASEService((ASEService) bindingConfiguration);

		channelConfigure.registerChannelListener(new ChannelListener(new BACnetEID(getDeviceId())) {

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
					new UnsignedInteger(55), new Real(45), new UnsignedInteger(1));
			final ByteQueue byteQueue = new ByteQueue();
			readRequest.write(byteQueue);
			final TPDU tpdu = new TPDU(new BACnetEID(120), new BACnetEID(1), byteQueue.popAll());

			final T_UnitDataRequest unitDataRequest = new T_UnitDataRequest(uri, tpdu, 1, null);

			aseServiceChannel.doRequest(unitDataRequest);

		} catch (Exception e) {
			System.err.print(e);
		}
	}

	public byte[] performRegisterOverBds(BACnetEID who, URI location, BACnetEID bds) {
		final SequenceOf<CharacterString> uriChars = new SequenceOf<CharacterString>();
		uriChars.add(new CharacterString(location.toString()));
		final AddListElementRequest request = new AddListElementRequest(
				new BACnetObjectIdentifier(BACnetObjectType.multiStateInput, 1), BACnetPropertyIdentifier.stateText,
				null, uriChars);
		final ByteQueue byteQueue = new ByteQueue();
		request.write(byteQueue);
		return byteQueue.popAll();
	}

	public void sendBacnetRegisterMessage() {
		try {
			ByteQueue queue = new ByteQueue();
			queue = new ByteQueue(performRegisterOverBds(new BACnetEID(getDeviceId()),
					new URI(SECURE_SCHEME + getHostAddress() + DTLS_PORT), AUTH_EID));
			final TPDU tpdu = new TPDU(new BACnetEID(getDeviceId()), AUTH_EID, queue.popAll());
			final T_UnitDataRequest unitDataRequest = new T_UnitDataRequest(new URI(SECURE_SCHEME + AUTH_IP + 5684),
					tpdu, 1, null);
			aseServiceChannel.doRequest(unitDataRequest);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	public void createCoapClient() {
		this.client = new CoapClient();
	}

	public void sendMessage(byte[] msg, String destinatonUrl) {

		Request request = new Request(Code.POST);
		request.setURI(destinatonUrl);
		request.setPayload(msg);
		CoapHandler messageHandler = new CoapHandler() {
			@Override
			public void onLoad(CoapResponse response) {
				byte[] msg = response.getPayload();
				if (msg[0] == OOBProtocol.DH2.getValue()) {
					Dh2Message dh2Message = new Dh2Message(msg);
					LOG.info("Dh2Message message received from authorizer with publicKey: {}",
							ByteArrayUtils.toHex(dh2Message.getPublicKeyBA()));
					LOG.info("received deviceId: " + dh2Message.getDeviceId());
					setDeviceId(dh2Message.getDeviceId());
					ecdhHelper.computeSharedSecret(dh2Message.getPublicKeyBA());
					setSignal(true);
				}
			}

			@Override
			public void onError() {

			}
		};
		client.advanced(messageHandler, request);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public synchronized void setSignal(boolean value) {
		signal = value;
		notifyAll();
	}

	public synchronized void waitForSignal() {
		while (signal != true) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			setSignal(false);
		}
	}

	public void setDeviceId(int deviceId) {
		this.deviceId = deviceId;
	}

	public int getDeviceId() {
		return this.deviceId;
	}

	public void hostAddress() {
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface i = interfaces.nextElement();
				if (i != null) {
					Enumeration<InetAddress> addresses = i.getInetAddresses();
					while (addresses.hasMoreElements()) {
						InetAddress address = addresses.nextElement();
						String hostAddr = address.getHostAddress();
						if (hostAddr.indexOf("139.") == 0) {
							setHostAddress(hostAddr);
						}
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	public void setHostAddress(String hostAddress) {
		this.hostAddress = hostAddress;
	}

	public String getHostAddress() {
		return this.hostAddress + ":";
	}
}