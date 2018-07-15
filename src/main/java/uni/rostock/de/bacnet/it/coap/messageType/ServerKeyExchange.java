package uni.rostock.de.bacnet.it.coap.messageType;

import org.eclipse.californium.elements.util.DatagramReader;
import org.eclipse.californium.elements.util.DatagramWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uni.rostock.de.bacnet.it.coap.crypto.OobAuthSession;

public class ServerKeyExchange {

	private static final Logger LOG = LoggerFactory.getLogger(ServerKeyExchange.class);
	private static final int MESSAGE_TYPE = OOBProtocol.SERVER_KEY_EXCHANGE.getValue();
	private final byte[] serverNonceBA;
	private final byte[] oobPswdIdBA;
	private final int deviceId;
	private final byte[] publicKeyBA;
	private final byte[] finalMessage;
	private boolean isMacVerified = false;

	public ServerKeyExchange(int deviceId, byte[] serverPubKey, OobAuthSession session) {
		serverNonceBA = session.getServerNonce();
		oobPswdIdBA = session.getOobPswdId();
		publicKeyBA = serverPubKey;
		this.deviceId = deviceId;
		DatagramWriter writer = new DatagramWriter();
		writer.write(MESSAGE_TYPE, 3);
		writer.writeBytes(serverNonceBA);
		writer.writeBytes(oobPswdIdBA);
		writer.write(deviceId, 32);
		writer.writeBytes(publicKeyBA);
		byte[] macData = calculateMac(session);
		writer.writeBytes(macData);
		this.finalMessage = writer.toByteArray();
		LOG.debug("ServerKeyExchange message serialized");
	}

	public ServerKeyExchange(OobAuthSession session, byte[] finalBA) {
		DatagramReader reader = new DatagramReader(finalBA);
		int messageType = reader.read(3);
		if (messageType != MESSAGE_TYPE) {
			LOG.debug("ServerKeyExchange authentication failed, wrong messageType received");
		}
		this.finalMessage = finalBA;
		serverNonceBA = reader.readBytes(OOBProtocol.NONCE_LENGTH.getValue());
		oobPswdIdBA = reader.readBytes(OOBProtocol.OOB_PSWD_ID_LENGTH.getValue());
		deviceId = reader.read(OOBProtocol.DEVICE_ID_LENGTH.getValue());
		publicKeyBA = new byte[OOBProtocol.PUBLIC_KEY_BYTES.getValue()];
		byte[] receviedMac = reader.readBytesLeft();
		byte[] calculatedMac = calculateMac(session);
		if (calculatedMac.equals(receviedMac)) {
			setMacVerified(true);
		} else {
			LOG.debug("ServerKeyExchange authentication failed, MAC verfication failed");
		}
	}

	public byte[] getPublicKeyBA() {
		return this.publicKeyBA;
	}

	public byte[] getBA() {
		return this.finalMessage;
	}

	public byte[] getOobPswdIdBA() {
		return this.oobPswdIdBA;
	}

	public int getDeviceId() {
		return this.deviceId;
	}

	private byte[] calculateMac(OobAuthSession session) {
		DatagramWriter writer = new DatagramWriter();
		writer.write(MESSAGE_TYPE, 3);
		writer.writeBytes(serverNonceBA);
		writer.writeBytes(oobPswdIdBA);
		writer.write(deviceId, 32);
		writer.writeBytes(publicKeyBA);
		return session.getMac(writer.toByteArray());
	}

	public boolean isMacVerified() {
		return isMacVerified;
	}

	public void setMacVerified(boolean isMacVerified) {
		this.isMacVerified = isMacVerified;
	}
}
