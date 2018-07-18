package uni.rostock.de.bacnet.it.coap.crypto;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uni.rostock.de.bacnet.it.coap.messageType.DeviceKeyExchange;
import uni.rostock.de.bacnet.it.coap.messageType.OobFinalMessage;
import uni.rostock.de.bacnet.it.coap.messageType.OobProtocol;
import uni.rostock.de.bacnet.it.coap.messageType.ServerKeyExchange;

public class OobClient extends CoapClient {

	private static Logger LOG = LoggerFactory.getLogger(OobClient.class.getCanonicalName());
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
		Thread handShakeThread = new Thread(new OobAuthHandler());
		handShakeThread.start();
	}

	class OobAuthHandler implements Runnable {

		Listener clientKeyExchangelistener = new Listener();
		Listener oobFinalMessageListener = new Listener();

		@Override
		public void run() {
			session.setClientNonce(ecdhHelper.getRandomBytes(OobProtocol.NONCE_LENGTH));
			session.setSalt(ecdhHelper.getRandomBytes(OobProtocol.SALT_LENGTH));
			session.deriveOobPswdKey(session.getOobPswdSalt());
			DeviceKeyExchange deviceKeyExchange = new DeviceKeyExchange(session, ecdhHelper.getPubKeyBytes());
			byte[] deviceKeyExchangeMessageBA = deviceKeyExchange.getBA();
			sendOobHandShakeMessage(clientKeyExchangelistener, deviceKeyExchangeMessageBA);
			while (!clientKeyExchangelistener.isDone()) {
				// TODO: check whether there is an error message
				if (clientKeyExchangelistener.receivedError()) {
					// TODO: resend the clientKeyExchange message
				}
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				// TODO: check whether the oob key life time is expired
				// if (isMessageLifeExpired()) {
				// // TODO: throw exception oob key for the given server expired
				// // blink the led indicating the failure of oob authentication
				// }
			}
			OobFinalMessage oobFinalMessage = new OobFinalMessage(session);
			sendOobHandShakeMessage(oobFinalMessageListener, oobFinalMessage.getBA());
			while (!oobFinalMessageListener.isDone()) {

			}
			//TODO: derive the secret key
			//TODO: add the secret key to dtls connector
			//TODO: add throttling on device side
			//TODO: add throttling on server side

		}
	}

	private void sendOobHandShakeMessage(Listener listener, byte[] payload) {
		post(new CoapHandler() {

			@Override
			public void onLoad(CoapResponse response) {
				LOG.info("device received a message");
				byte[] messageBA = response.getPayload();
				if (response.isSuccess()) {
					int firstByte = messageBA[0];
					if ((firstByte >> 5) == OobProtocol.SERVER_KEY_EXCHANGE) {
						LOG.info("device received ServerKeyExchange message");
						ServerKeyExchange serverKeyExchange = new ServerKeyExchange(session, messageBA);
						if (session.isServerKeyExchangeMessageAuthenticated(serverKeyExchange)) {
							ecdhHelper.computeSharedSecret(serverKeyExchange.getPublicKeyBA());
							LOG.info("server key message has been authenticated");
							listener.onResponse(true);
						} else {
							LOG.info("authenticating server key message failed");
						}
					} else {
						LOG.info("success response received for OobFinalMessage from server");
						listener.onResponse(true);
					}
				}
			}

			@Override
			public void onError() {
				listener.isDone();
			}
		}, payload, 0);
		// setMessageSentTime();
	}

	private class Listener {

		private Integer result;
		private boolean done;
		private byte[] messageReceived;
		private boolean receivedError = false;

		public void onResponse(boolean val) {
			this.done = val;
		}

		public void setReceivedError() {
			this.receivedError = true;
		}

		public void setMessageReceived(byte[] message) {
			this.messageReceived = message;
		}

		public boolean isDone() {
			// LOG.info(this.done+" ");
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

	// private boolean isMessageLifeExpired() {
	// int delay = (int) ((System.nanoTime() - this.messageSentTimeStamp) /
	// 1000000000);
	// if (delay < 180) {
	// return true;
	// } else {
	// return false;
	// }
	// }
}
