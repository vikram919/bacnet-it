package uni.rostock.de.bacnet.it.coap.examples;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.SecureRandom;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.elements.util.DatagramReader;
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
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.Encodable;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.constructed.SequenceOf;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.constructed.ServicesSupported;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.primitive.OctetString;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.primitive.UnsignedInteger;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.util.ByteQueue;
import ch.fhnw.bacnetit.samplesandtests.api.service.confirmed.AddListElementRequest;
import ch.fhnw.bacnetit.samplesandtests.api.service.confirmed.WritePropertyRequest;
import uni.rostock.de.bacnet.it.coap.crypto.EcdhHelper;
import uni.rostock.de.bacnet.it.coap.crypto.OobAuthSession;
import uni.rostock.de.bacnet.it.coap.messageType.DeviceKeyExchange;
import uni.rostock.de.bacnet.it.coap.messageType.OobProtocol;
import uni.rostock.de.bacnet.it.coap.messageType.OobSessionsStore;
import uni.rostock.de.bacnet.it.coap.messageType.ServerKeyExchange;
import uni.rostock.de.bacnet.it.coap.transportbinding.TransportDTLSCoapBinding;

public class Authorizer {

	private static final Logger LOG = LoggerFactory.getLogger(Authorizer.class);
	private static final int AUTH_ID = 1;
	private static final int MOBILE_ID = 2;
	private static final int DEVICE_ID = 120;
	private static final String AUTH_IP = "139.30.202.56:";
	private ASEServices aseServices;
	private ApplicationService aseService;
	private static final String SECURE_SCHEME = "coaps://";
	private static final int DTLS_SOCKET = 5684;
	private static final int COAP_PORT = 5683;
	private SecureRandom random = new SecureRandom();
	private TransportDTLSCoapBinding coapDtlsbindingConfig;
	private static final int GROUP1_ID = 445;
	private String oobKeyPswd = "";
	private static final int ALLOWED_FAIL_ATTEMPTS = 2;
	private static String OOB_PSWD_STRING = "10101110010101101011";
	private static OobSessionsStore deviceSessionsMap = OobSessionsStore.getInstance();

	public static void main(String[] args) {

		Authorizer authdevice = new Authorizer();
		// authdevice.createCoapServer(COAP_PORT);
		final DiscoveryConfig ds = new DiscoveryConfig("DNSSD", "1.1.1.1", "itb.bacnet.ch.", "bds._sub._bacnet._tcp.",
				"dev._sub._bacnet._tcp.", "obj._sub._bacnet._tcp.", false);

		try {
			DirectoryService.init();
			DirectoryService.getInstance().setDNSBinding(new DNSSD(ds));

		} catch (final Exception e1) {
			e1.printStackTrace();
		}
		authdevice.start();

		// try {
		// DiscoveryConfig ds = new DiscoveryConfig("DNSSD", "1.1.1.1",
		// "itb.bacnet.ch.", "bds._sub._bacnet._udp.",
		// "auth._sub._bacnet._udp.", "authservice._sub._bacnet._udp.", false);
		// DirectoryService.init();
		// DirectoryService.getInstance().setDNSBinding(new DNSSD(ds));
		// DirectoryService.getInstance().register(new BACnetEID(AUTH_ID),
		// new URI(SECURE_SCHEME + AUTH_IP + +DTLS_SOCKET), true, false);
		// LOG.info("This device is registered as BDS !");
		// } catch (final Exception e1) {
		// e1.printStackTrace();
		// }
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
		coapDtlsbindingConfig.createSecureCoapServer(DTLS_SOCKET);

		coapDtlsbindingConfig.init();

		channelConfigure.setASEService((ASEService) coapDtlsbindingConfig);

		channelConfigure.registerChannelListener(new ChannelListener(new BACnetEID(AUTH_ID)) {

			@Override
			public void onIndication(T_UnitDataIndication arg0, Object arg1) {

				LOG.debug("message T_unitDataIndication");
				ASDU receivedRequest = getServiceFromBody(arg0.getData().getBody());
				if (receivedRequest instanceof ConfirmedRequest
						&& ((ConfirmedRequest) receivedRequest).getServiceRequest() instanceof WritePropertyRequest) {
					LOG.debug("authorizer received a WritePropertyRequest!");
					ByteQueue queue = new ByteQueue(arg0.getData().getBody());
					byte[] msg = queue.peek(15, queue.size() - 21);
					System.out.println(new String(msg));

					if (msg[0] == OobProtocol.ADD_DEVICE_REQUEST) {
						LOG.info("Auth received add device request from mobile!!");
						for (int i = 0; i < 20; i++) {
							oobKeyPswd += random.nextInt(2);
						}
						setOobPswdKeyString(oobKeyPswd);
					}
				} else if (receivedRequest instanceof ConfirmedRequest
						&& ((ConfirmedRequest) receivedRequest).getServiceRequest() instanceof AddListElementRequest) {

					SequenceOf<?> charcterStrings = ((AddListElementRequest) ((ConfirmedRequest) receivedRequest)
							.getServiceRequest()).getListOfElements();
					LOG.info("BDS got an AddListElementRequest from device: "
							+ arg0.getData().getSourceEID().getIdentifierAsString());
					for (Encodable cs : charcterStrings) {
						try {
							DirectoryService.getInstance().register(arg0.getData().getSourceEID(),
									new URI(cs.toString()), false, false);
							if (arg0.getData().getSourceEID().equals(new BACnetEID(DEVICE_ID))) {
								sendWritePropertyRequest("Hi this is Authorizer!".getBytes(), DEVICE_ID,
										"test message");
							}
						} catch (URISyntaxException e) {
							e.printStackTrace();
						}
					}
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
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

	public void createCoapServer(int serverPort) {
		CoapServer oobAuthServer = new CoapServer(5683);
		deviceSessionsMap.addDeviceoobPswd(OOB_PSWD_STRING);
		oobAuthServer.add(new CoapResource("authentication") {
			@Override
			public void handlePOST(CoapExchange exchange) {
				byte[] msg = exchange.getRequestPayload();
				DatagramReader reader = new DatagramReader(msg);
				int first3Bits = reader.read(3);
				LOG.info("authorizer recevived message from device of type: {}", first3Bits);
				if (first3Bits == OobProtocol.DEVICE_KEY_EXCHANGE) {
					DeviceKeyExchange deviceKeyExchange = new DeviceKeyExchange(msg);
					if (deviceSessionsMap.hasOobPswdId(deviceKeyExchange.getOobPswdIdBA())) {
						OobAuthSession oobAuthSession = deviceSessionsMap
								.getAuthSession(deviceKeyExchange.getOobPswdIdBA());
						if (oobAuthSession.getFailedAuthAttempts() < ALLOWED_FAIL_ATTEMPTS) {
							LOG.info("DeviceKeyExchange message received is within allowed fail attempts");
							processDKEMessage(oobAuthSession, deviceKeyExchange, exchange);
						}
						if (oobAuthSession.getFailedAuthAttempts() > ALLOWED_FAIL_ATTEMPTS) {
							LOG.info("DeviceKeyExchange message received is after allowed fail attempts");
							if (isThortlingTimeFinished(oobAuthSession)) {
								LOG.info("Throttling time is finished, device processing DeviceKeyExchange message");
								processDKEMessage(oobAuthSession, deviceKeyExchange, exchange);
							} else {
								LOG.info("DevicekeyExchange message discarded due to throttling effect");
							}
						}
					} else {
						LOG.info("no OobAuthSession exists for the received OobPswdId");
					}
				}
			}
		});
		oobAuthServer.start();
	}

	public void sendWritePropertyRequest(byte[] message, int toDeviceId, String context) {
		WritePropertyRequest writePropertyRequest = new WritePropertyRequest(
				new BACnetObjectIdentifier(BACnetObjectType.analogValue, 1), BACnetPropertyIdentifier.presentValue,
				new UnsignedInteger(55), new OctetString(message), new UnsignedInteger(1));
		final ByteQueue byteQueue = new ByteQueue();
		writePropertyRequest.write(byteQueue);

		final TPDU tpdu = new TPDU(new BACnetEID(AUTH_ID), new BACnetEID(toDeviceId), byteQueue.popAll());

		T_UnitDataRequest unitDataRequest = null;
		try {
			unitDataRequest = new T_UnitDataRequest(DirectoryService.getInstance().resolve(new BACnetEID(toDeviceId)),
					tpdu, 1, context);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		aseService.doRequest(unitDataRequest);
	}

	public void setOobPswdKeyString(String key) {
		this.oobKeyPswd = key;
	}

	public String getOobPswdKeyString() {
		return this.oobKeyPswd;
	}

	private void processDKEMessage(OobAuthSession session, DeviceKeyExchange deviceKeyExchange, CoapExchange exchange) {
		LOG.info("DeviceKeyExchange message received is within allowed fail attempts");
		if (session.isDeviceKeyExchangeMessageAuthenticated(deviceKeyExchange)) {
			LOG.info("device is authenticated");
			LOG.info("sending server key exchange message to the device");
			session.setServerNonce(deviceSessionsMap.getServerNonce());
			session.setClientNonce(deviceKeyExchange.getDeviceNonce());
			session.setDeviceId(200);
			ServerKeyExchange serverKeyExchange = new ServerKeyExchange(session.getDeviceId(),
					deviceSessionsMap.getPubKey(), session);
			byte[] sharedSecret = deviceSessionsMap.computeSharedSecret(session.getForeignPublicKey());
			/* adding the master secret to InMemoryPreSharedKeyStore */
			coapDtlsbindingConfig.addPSK(new String(Integer.toString(session.getDeviceId())), sharedSecret,
					new InetSocketAddress(exchange.getSourceAddress(), DTLS_SOCKET));
			exchange.respond(ResponseCode._UNKNOWN_SUCCESS_CODE, serverKeyExchange.getBA());
		} else {
			session.incrementFailedAuthAttempts();
			session.setThrottlingInitTime(System.nanoTime());
			LOG.info("authenticating device key message failed");
		}
	}

	private boolean isThortlingTimeFinished(OobAuthSession session) {
		int delay = (int) ((System.nanoTime() - session.getThrottlingInitTime()) / 1000000000);
		if (delay > Math.pow(2, session.getFailedAuthAttempts())) {
			return true;
		}
		return false;
	}
}
