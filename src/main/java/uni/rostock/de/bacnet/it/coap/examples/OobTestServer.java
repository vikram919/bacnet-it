package uni.rostock.de.bacnet.it.coap.examples;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.elements.util.DatagramReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uni.rostock.de.bacnet.it.coap.crypto.OobAuthSession;
import uni.rostock.de.bacnet.it.coap.messageType.DeviceKeyExchange;
import uni.rostock.de.bacnet.it.coap.messageType.OobProtocol;
import uni.rostock.de.bacnet.it.coap.messageType.OobSessionsStore;
import uni.rostock.de.bacnet.it.coap.messageType.ServerKeyExchange;

public class OobTestServer {
	private static Logger LOG = LoggerFactory.getLogger(OobTestServer.class.getCanonicalName());
	private static final int ALLOWED_FAIL_ATTEMPTS = 2;
	private static String OOB_PSWD_STRING = "10101110010101101011";
	private static OobSessionsStore deviceSessionsMap = OobSessionsStore.getInstance();

	public static void main(String[] args) {
		OobTestServer oobTestServer = new OobTestServer();
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
							oobTestServer.processDKEMessage(oobAuthSession, deviceKeyExchange, exchange);
						}
						if (oobAuthSession.getFailedAuthAttempts() > ALLOWED_FAIL_ATTEMPTS) {
							LOG.info("DeviceKeyExchange message received is after allowed fail attempts");
							if (oobTestServer.isThortlingTimeFinished(oobAuthSession)) {
								LOG.info("Throttling time is finished, device processing DeviceKeyExchange message");
								oobTestServer.processDKEMessage(oobAuthSession, deviceKeyExchange, exchange);
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
			ServerKeyExchange serverKeyExchange = new ServerKeyExchange(200, deviceSessionsMap.getPubKey(), session);
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
