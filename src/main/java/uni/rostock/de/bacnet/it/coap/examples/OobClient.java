package uni.rostock.de.bacnet.it.coap.examples;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.elements.util.DatagramReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uni.rostock.de.bacnet.it.coap.crypto.EcdhHelper;
import uni.rostock.de.bacnet.it.coap.crypto.OobAuthSession;
import uni.rostock.de.bacnet.it.coap.messageType.DeviceKeyExchange;
import uni.rostock.de.bacnet.it.coap.messageType.OOBProtocol;
import uni.rostock.de.bacnet.it.coap.messageType.ServerKeyExchange;

public class OobClient extends CoapClient {

	private static Logger LOG = LoggerFactory.getLogger(OobTestClient.class.getCanonicalName());
	private final EcdhHelper ecdhHelper = new EcdhHelper(0);
	private final OobAuthSession session;
	private int ClientKeyExchangeStatus;
	private int isClientKeyExchangeSuccess;
	private double messageSentTimeStamp;
	private int delay;

	public OobClient(String oobPswdString, String uri) {
		super(uri);
		session = new OobAuthSession(ecdhHelper, oobPswdString);
	}

	public void startHandShake() {

	}

	class OobAuthHandler implements Runnable {
		
		Listener clientKeyExchangelistener = new Listener();
		Listener oobFinalMessageListener = new Listener();

		@Override
		public void run() {
			session.setClientNonce(ecdhHelper.getRandomBytes(OOBProtocol.NONCE_LENGTH.getValue()));
			session.setSalt(ecdhHelper.getRandomBytes(OOBProtocol.SALT_LENGTH.getValue()));
			session.deriveOobPswdKey(session.getOobPswdSalt());
			DeviceKeyExchange deviceKeyExchange = new DeviceKeyExchange(session, ecdhHelper.getPubKeyBytes());
			byte[] deviceKeyExchangeMessageBA = deviceKeyExchange.getBA();
			sendOobHandShakeMessage(clientKeyExchangelistener, deviceKeyExchangeMessageBA);
			while (!clientKeyExchangelistener.isDone()) {
				// TODO: check whether there is an error message
				if (clientKeyExchangelistener.receivedError()) {
					// TODO: resend the clientKeyExchange message
				}
				// TODO: check whether the oob key life time is expired
//				if (isMessageLifeExpired()) {
//					// TODO: throw exception oob key for the given server expired
//					// blink the led indicating the failure of oob authentication
//				}
			}
//			OobFinalMessage oobFinalMessage = new OobFinalMessage(session);
//			sendOobHandShakeMessage(oobFinalMessageListener, oobFinalMessage.getBA());
//			while () {
//				
//			}
			// TODO: send the OobFinalMessage
		}
	}

	private void sendOobHandShakeMessage(Listener listener, byte[] payload) {
		post(new CoapHandler() {

			@Override
			public void onLoad(CoapResponse response) {
				byte[] messageBA = response.getPayload();
				DatagramReader reader = new DatagramReader(messageBA);
				if (reader.read(3) == OOBProtocol.SERVER_KEY_EXCHANGE.getValue()) {
					ServerKeyExchange serverKeyExchange = new ServerKeyExchange(session, messageBA);
					if (serverKeyExchange.isMacVerified()) {
						ecdhHelper.computeSharedSecret(serverKeyExchange.getPublicKeyBA());
						listener.isDone();
					}
				}
			}

			@Override
			public void onError() {
				listener.isDone();
			}
		}, payload, 0);
		setMessageSentTime();
	}

	private static class Listener {
		private Integer result;
		private boolean done;
		private byte[] messageReceived;
		private boolean receivedError = false;

		public void onResult(Integer result) {
			this.result = result;
			this.done = true;
		}

		public void setReceivedError() {
			this.receivedError = true;
		}

		public void setMessageReceived(byte[] message) {
			this.messageReceived = message;
		}

		public boolean isDone() {
			return done;
		}

		public Integer getResult() {
			return result;
		}

		public boolean receivedError() {
			return receivedError;
		}

		public byte[] getReceivedMessage() {
			return messageReceived;
		}
	}

	private void setMessageSentTime() {
		this.messageSentTimeStamp = System.nanoTime();
	}

//	private boolean isMessageLifeExpired() {
//		int delay = (int) ((System.nanoTime() - this.messageSentTimeStamp) / 1000000000);
//		if (delay < 180) {
//			return true;
//		} else {
//			return false;
//		}
//	}
}
