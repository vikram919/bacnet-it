package uni.rostock.de.bacnet.it.coap.examples;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uni.rostock.de.bacnet.it.coap.crypto.OobAuthSession;
import uni.rostock.de.bacnet.it.coap.messageType.DeviceKeyExchange;
import uni.rostock.de.bacnet.it.coap.messageType.OobProtocol;
import uni.rostock.de.bacnet.it.coap.messageType.OobSessionsStore;
import uni.rostock.de.bacnet.it.coap.messageType.ServerKeyExchange;

public class OobTestServer {
	private static Logger LOG = LoggerFactory.getLogger(OobTestServer.class.getCanonicalName());
	private static String OOB_PSWD_STRING = "10101110010101101011";
	private static OobSessionsStore deviceSessionsMap = OobSessionsStore.getInstance();
	private OobAuthSession oobAuthSession;

	public static void main(String[] args) {
		OobTestServer oobTestServer = new OobTestServer();
		CoapServer oobAuthServer = new CoapServer(5683);
		deviceSessionsMap.addDeviceoobPswd(OOB_PSWD_STRING);
		oobAuthServer.add(new CoapResource("authentication") {
			@Override
			public void handlePOST(CoapExchange exchange) {
				LOG.info("authorizer recevived message from device");
				byte[] msg = exchange.getRequestPayload();
				int firstByte = msg[0];
				switch (firstByte >> 5) {
				case OobProtocol.DEVICE_KEY_EXCHANGE:
					DeviceKeyExchange deviceKeyExchange = new DeviceKeyExchange(msg);
					if (deviceSessionsMap.hasOobPswdId(deviceKeyExchange.getOobPswdIdBA())) {
						LOG.info("authorizer recevived DeviceKeyExchangeMessage from device");
						oobTestServer.oobAuthSession = deviceSessionsMap
								.getAuthSession(deviceKeyExchange.getOobPswdIdBA());
						if (oobTestServer.oobAuthSession.isDeviceKeyExchangeMessageAuthenticated(deviceKeyExchange)) {
							LOG.info("device is authenticated");
							LOG.info("sending server key exchange message to the device");
							oobTestServer.oobAuthSession.setServerNonce(deviceSessionsMap.getServerNonce());
							oobTestServer.oobAuthSession.setClientNonce(deviceKeyExchange.getDeviceNonce());
							ServerKeyExchange serverKeyExchange = new ServerKeyExchange(200,
									deviceSessionsMap.getPubKey(), oobTestServer.oobAuthSession);
							exchange.respond(ResponseCode._UNKNOWN_SUCCESS_CODE, serverKeyExchange.getBA());
						}
					}

					break;
				case 4:
					break;
				}
				if (firstByte >> 5 == OobProtocol.DEVICE_KEY_EXCHANGE) {

				}
			}
		});
		oobAuthServer.start();

	}

}
