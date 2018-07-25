package uni.rostock.de.bacnet.it.coap.oobAuth;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uni.rostock.de.bacnet.it.coap.crypto.EcdhHelper;
import uni.rostock.de.bacnet.it.coap.transportbinding.TransportDTLSCoapBinding;

public class OobAuthClient extends CoapClient {

	private static Logger LOG = LoggerFactory.getLogger(OobAuthClient.class.getCanonicalName());
	private static final int ALLOWED_FAIL_ATTEMPTS = 2;

	private final EcdhHelper ecdhHelper = new EcdhHelper(0);
	private final OobAuthSession session;
	private final TransportDTLSCoapBinding bindingConfiguration;

	public OobAuthClient(String oobPswdString, String uri, TransportDTLSCoapBinding bindingConfiguration) {
		super(uri);
		this.bindingConfiguration = bindingConfiguration;
		session = new OobAuthSession(ecdhHelper, oobPswdString);
	}

	public void startHandShake() {
		
		session.setClientNonce(ecdhHelper.getRandomBytes(OobProtocol.NONCE_LENGTH));
		session.setSalt(ecdhHelper.getRandomBytes(OobProtocol.SALT_LENGTH));
		session.deriveOobPswdKey(session.getOobPswdSalt());
		DeviceKeyExchange deviceKeyExchange = new DeviceKeyExchange(session, ecdhHelper.getPubKeyBytes());
		byte[] deviceKeyExchangeMessageBA = deviceKeyExchange.getBA();
		sendOobHandShakeMessage(deviceKeyExchangeMessageBA);
		
		
		LOG.info("device successfully authenticated ServerKeyExchange message from server");
		byte[] sharedSecret = ecdhHelper.computeSharedSecret(session.getForeignPublicKey());
		try {
			int deviceId = session.getDeviceId();
			bindingConfiguration.addPSK(Integer.toString(deviceId), sharedSecret,
					new InetSocketAddress(InetAddress.getByName(new URI(getURI()).getHost()), CoAP.DEFAULT_COAP_SECURE_PORT));
		} catch (UnknownHostException | URISyntaxException e) {
			e.printStackTrace();
		}
		LOG.info("device have established a secret key with server, and is added to InMemoryPSKStore");
	}

	private void sendOobHandShakeMessage(byte[] payload) {
		CountDownLatch latch = new CountDownLatch(1);
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
							processingSKEMessage(latch, messageBA);
						}
						if (session.getFailedAuthAttempts() > ALLOWED_FAIL_ATTEMPTS) {
							LOG.info("ServerKeyExchange message received is after allowed fail attempts");
							if (isThortlingTimeFinished()) {
								LOG.info("Throttling time is finished, device processing ServerKeyExchange message");
								processingSKEMessage(latch, messageBA);
							} else {
								LOG.info("ServerkeyExchange message discarded due to throttling effect");
							}
						}
					} else {
						LOG.info("discarding unknown message received");
					}
				} else {
					try {
						throw new Exception("received response does not carry success code");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			@Override
			public void onError() {
				try {
					throw new Exception("error response received");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, payload, 0);
		
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
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

	private void processingSKEMessage(CountDownLatch latch, byte[] messageBA) {
		LOG.info("device received ServerKeyExchange message");
		ServerKeyExchange serverKeyExchange = new ServerKeyExchange(session, messageBA);
		if (session.isServerKeyExchangeMessageAuthenticated(serverKeyExchange)) {
			session.setForeignPublicKey(serverKeyExchange.getPublicKeyBA());
			LOG.info("server key message has been authenticated");
			latch.countDown();
		} 
//		else {
//			session.incrementFailedAuthAttempts();
//			session.setThrottlingInitTime(System.nanoTime());
//			LOG.info("authenticating server key message failed");
//		}
		else {
			try {
				throw new Exception("server key exchange authentication failed");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public int getDeviceId() {
		return session.getDeviceId();
	}
}
