package uni.rostock.de.bacnet.it.coap.crypto;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uni.rostock.de.bacnet.it.coap.messageType.DeviceKeyExchange;
import uni.rostock.de.bacnet.it.coap.messageType.OobProtocol;
import uni.rostock.de.bacnet.it.coap.messageType.OobProtocolException;
import uni.rostock.de.bacnet.it.coap.messageType.ServerKeyExchange;

public class OobClient extends CoapClient {

	private static Logger LOG = LoggerFactory.getLogger(OobClient.class.getCanonicalName());
	private static final int ALLOWED_FAIL_ATTEMPTS = 2;

	private final EcdhHelper ecdhHelper = new EcdhHelper(0);
	private final OobAuthSession session;

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

				if (clientKeyExchangelistener.receivedError()) {
					sendOobHandShakeMessage(clientKeyExchangelistener, deviceKeyExchangeMessageBA);
				}
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (isOobPswdLifeExpired()) {
					try {
						throw new OobProtocolException(
								"OobPswd life time expired, device state needs to be switched to initial state");
					} catch (OobProtocolException e) {
						e.printStackTrace();
					}
					break;
				}
			}
			LOG.info("device successfully authenticated ServerKeyExchange message from server");
			ecdhHelper.computeSharedSecret(session.getForeignPublicKey());
			LOG.info("device have established a secret key with server");
			// TODO: add throttling on server side
			// TODO: add the secret key to dtls connector

		}
	}

	private void sendOobHandShakeMessage(Listener listener, byte[] payload) {
		post(new CoapHandler() {

			@Override
			public void onLoad(CoapResponse response) {
				LOG.info("device received a message");
				if (response.isSuccess()) {
					LOG.info("received message has payload and carries success response code");
					byte[] messageBA = response.getPayload();
					int firstByte = messageBA[0];
					int first3Bits = firstByte >> 5;
					LOG.info("received message type: " + first3Bits);
					// LSB 3 bits of firstByte defines the type of message received
					if (first3Bits == OobProtocol.SERVER_KEY_EXCHANGE) {
						LOG.info("received ServerKeyExchange message");
						if (session.getFailedAuthAttempts() < ALLOWED_FAIL_ATTEMPTS) {
							LOG.info("ServerKeyExchange message received is within allowed fail attempts");
							processingSKEMessage(listener, messageBA);
						}
						if (session.getFailedAuthAttempts() > ALLOWED_FAIL_ATTEMPTS) {
							LOG.info("ServerKeyExchange message received is after allowed fail attempts");
							if (isThortlingTimeFinished()) {
								LOG.info("Throttling time is finished, device processing ServerKeyExchange message");
								processingSKEMessage(listener, messageBA);
							} else {
								LOG.info("ServerkeyExchange message discarded due to throttling effect");
							}
						}
					} else {
						LOG.info("discarding unknown message received");
					}
				} else {
					LOG.info("received response does not carry success code, resending the message");
					listener.setReceivedError();
				}
			}

			@Override
			public void onError() {
				listener.setReceivedError();
			}
		}, payload, 0);
	}

	private class Listener {

		private boolean done;
		private boolean receivedError = false;

		public void onResponse(boolean val) {
			this.done = val;
		}

		public void setReceivedError() {
			receivedError = true;
		}

		public boolean isDone() {
			// LOG.info(this.done+" ");
			return done;
		}

		public boolean receivedError() {
			return receivedError;
		}
	}

	private boolean isOobPswdLifeExpired() {
		int delay = (int) ((System.nanoTime() - session.getOobPswdCreatedTime()) / 1000000000);
		if (delay > 360) {
			return true;
		}
		return false;
	}

	private boolean isThortlingTimeFinished() {
		int delay = (int) ((System.nanoTime() - session.getThrottlingInitTime()) / 1000000000);
		if (delay > Math.pow(2, session.getFailedAuthAttempts())) {
			return true;
		}
		return false;
	}

	private void processingSKEMessage(Listener listener, byte[] messageBA) {
		LOG.info("device received ServerKeyExchange message");
		ServerKeyExchange serverKeyExchange = new ServerKeyExchange(session, messageBA);
		if (session.isServerKeyExchangeMessageAuthenticated(serverKeyExchange)) {
			session.setForeignPublicKey(serverKeyExchange.getPublicKeyBA());
			LOG.info("server key message has been authenticated");
			listener.onResponse(true);
		} else {
			session.incrementFailedAuthAttempts();
			session.setThrottlingInitTime(System.nanoTime());
			LOG.info("authenticating server key message failed");
		}
	}
}
