package uni.rostock.de.bacnet.it.coap.messageType;

import org.eclipse.californium.elements.util.DatagramReader;
import org.eclipse.californium.elements.util.DatagramWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uni.rostock.de.bacnet.it.coap.crypto.OobAuthSession;

public class DeviceKeyExchange {

	private static final Logger LOG = LoggerFactory.getLogger(DeviceKeyExchange.class);
	private static final int MESSAGE_TYPE = OOBProtocol.DEVICE_KEY_EXCHANGE.getValue();
	private final byte[] oobPswdIdBA;
	private final byte[] saltBA;
	private final byte[] clientNonce;
	private final byte[] publicKeyBA;
	private final byte[] finalMessage;

	public DeviceKeyExchange(OobAuthSession session, byte[] devicePubKey) {
		if (session == null) {
			throw new NullPointerException("session cannot be null");
		}
		if (devicePubKey == null) {
			throw new NullPointerException("device public key cannot be null");
		}
		oobPswdIdBA = session.getOobPswdId();
		if (session.getOobPswdSalt() == null) {
			throw new NullPointerException(
					"salt cannot be null in deviceKeyExchange message, consider to set Oob password salt on current session");
		}
		saltBA = session.getOobPswdSalt();
		if (session.getdeviceNonce() == null) {
			throw new NullPointerException(
					"client nonce cannot be null in deviceKeyExchange message, consider to set client nonce on current session");
		}
		clientNonce = session.getdeviceNonce();
		publicKeyBA = devicePubKey;
		DatagramWriter writer = new DatagramWriter();
		writer.write(MESSAGE_TYPE, 3);
		writer.writeBytes(oobPswdIdBA);
		writer.writeBytes(saltBA);
		writer.writeBytes(publicKeyBA);
		byte[] macData = session.getMac(writer.toByteArray());
		writer.writeBytes(macData);
		finalMessage = writer.toByteArray();
		LOG.debug("message serilaized to BA");
	}

	public DeviceKeyExchange(byte[] finalBA, OobAuthSession session) {
		DatagramReader reader = new DatagramReader(finalBA);
		int messageType = reader.read(3);
		if (messageType != MESSAGE_TYPE) {
			LOG.info("DeviceKeyExchange authentication failed, wrong messagetype received expected {} but received {1}",
					MESSAGE_TYPE, messageType);
		}
		finalMessage = finalBA;
		oobPswdIdBA = reader.readBytes(OOBProtocol.OOB_PSWD_KEY_LENGTH.getValue());
		saltBA = reader.readBytes(OOBProtocol.SALT_LENGTH.getValue());
		session.setSalt(saltBA);
		clientNonce = reader.readBytes(OOBProtocol.NONCE_LENGTH.getValue());
		session.setClientNonce(clientNonce);
		publicKeyBA = reader.readBytes(OOBProtocol.PUBLIC_KEY_BYTES.getValue());
		byte[] receivedMac = reader.readBytesLeft();
		/* construct mac from received message */
		DatagramWriter writer = new DatagramWriter();
		writer.write(MESSAGE_TYPE, 3);
		writer.writeBytes(oobPswdIdBA);
		writer.writeBytes(saltBA);
		writer.writeBytes(publicKeyBA);
		byte[] constructedMac = session.getMac(writer.toByteArray());
		if (!receivedMac.equals(constructedMac)) {
			LOG.debug("DeviceKeyExchange authentication failed, MAC verification failed");
		}
		// TODO: verify the mac, if failed discard the message
		LOG.debug("message deserialized");
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
}
