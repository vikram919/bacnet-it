package uni.rostock.de.bacnet.it.coap.oobAuth;

import java.net.InetSocketAddress;
import java.net.URI;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.elements.util.DatagramReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.fhnw.bacnetit.ase.application.service.api.ASEServices;
import ch.fhnw.bacnetit.ase.encoding.api.BACnetEID;
import ch.fhnw.bacnetit.ase.encoding.api.T_UnitDataRequest;
import uni.rostock.de.bacnet.it.coap.transportbinding.TransportDTLSCoapBinding;

public class OobAuthServer {

	private static final Logger LOG = LoggerFactory.getLogger(OobAuthServer.class.getCanonicalName());
	private static final int ALLOWED_FAIL_ATTEMPTS = 2;
	private final OobSessionsStore deviceSessionsMap;
	private final ASEServices aseServices;
	private final TransportDTLSCoapBinding coapDtlsbindingConfig;

	public OobAuthServer(TransportDTLSCoapBinding coapDtlsbindingConfig, OobSessionsStore deviceSessionsMap,
			ASEServices aseServices) {
		this.coapDtlsbindingConfig = coapDtlsbindingConfig;
		this.deviceSessionsMap = deviceSessionsMap;
		this.aseServices = aseServices;
	}

	public void startAuthServer(int serverPort) {
		CoapServer oobAuthServer = new CoapServer(5683);
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

	private void processDKEMessage(OobAuthSession session, DeviceKeyExchange deviceKeyExchange, CoapExchange exchange) {
		LOG.info("DeviceKeyExchange message received is within allowed fail attempts");
		if (session.isDeviceKeyExchangeMessageAuthenticated(deviceKeyExchange)) {
			LOG.info("device is authenticated");
			LOG.info("sending server key exchange message to the device");
			session.setServerNonce(deviceSessionsMap.getServerNonce());
			session.setClientNonce(deviceKeyExchange.getDeviceNonce());
			session.setForeignPublicKey(deviceKeyExchange.getPublicKeyBA());
			session.setDeviceId(200);
			ServerKeyExchange serverKeyExchange = new ServerKeyExchange(session.getDeviceId(),
					deviceSessionsMap.getPubKey(), session);
			byte[] sharedSecret = deviceSessionsMap.computeSharedSecret(session.getForeignPublicKey());
			/* adding the master secret to InMemoryPreSharedKeyStore */
			coapDtlsbindingConfig.addPSK(new String(Integer.toString(session.getDeviceId())), sharedSecret,
					new InetSocketAddress(exchange.getSourceAddress(), CoAP.DEFAULT_COAP_SECURE_PORT));
			// delete the OobAuthsession and remove the OOb password
			deviceSessionsMap.deleteSession(deviceKeyExchange.getOobPswdIdBA());
			LOG.debug("OobAuthSession deleted on server side");
			String mobileAddress = "coaps://"+session.getMobileAddress().getAddress()+":"+CoAP.DEFAULT_COAP_SECURE_PORT;
			OobStatus oobStatus = new OobStatus(session.getOobPswdId(), true);
			// TODO: add oobpswd id to oobstatus and add device request.
			ApplicationMessages.sendWritePropertyRequest(aseServices, oobStatus.getBA(), new BACnetEID(2),
					new BACnetEID(1), mobileAddress);
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
