package uni.rostock.de.bacnet.it.coap.messageType;

import org.eclipse.californium.elements.util.DatagramReader;
import org.eclipse.californium.elements.util.DatagramWriter;
import org.eclipse.californium.scandium.util.ByteArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uni.rostock.de.bacnet.it.coap.crypto.OobAuthSession;

public class DeviceKeyExchange {

	private static final Logger LOG = LoggerFactory.getLogger(DeviceKeyExchange.class.getCanonicalName());
	private static final int MESSAGE_TYPE = OobProtocol.DEVICE_KEY_EXCHANGE;
	private final byte[] oobPswdIdBA;
	private final byte[] saltBA;
	private final byte[] deviceNonce;
	private final byte[] publicKeyBA;
	private final byte[] finalMessage;
	private final byte[] messageMac;

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
		LOG.info("device salt: "+ ByteArrayUtils.toHex(saltBA));
		if (session.getdeviceNonce() == null) {
			throw new NullPointerException(
					"client nonce cannot be null in deviceKeyExchange message, consider to set client nonce on current session");
		}
		deviceNonce = session.getdeviceNonce();
		publicKeyBA = devicePubKey;
		DatagramWriter writer = new DatagramWriter();
		writer.write(MESSAGE_TYPE, 3);
		writer.writeBytes(oobPswdIdBA);
		writer.writeBytes(saltBA);
		writer.writeBytes(deviceNonce);		
		writer.writeBytes(publicKeyBA);
		messageMac = calculateMac(session);
		writer.writeBytes(messageMac);
		finalMessage = writer.toByteArray();
		LOG.debug("message serilaized to BA");
	}

	public DeviceKeyExchange(byte[] finalBA) {
		DatagramReader reader = new DatagramReader(finalBA);
		int messageType = reader.read(3);
		if (messageType != MESSAGE_TYPE) {
			LOG.info("DeviceKeyExchange authentication failed, wrong messagetype received expected {} but received {1}",
					MESSAGE_TYPE, messageType);
		}
		finalMessage = finalBA;
		oobPswdIdBA = reader.readBytes(OobProtocol.OOB_PSWD_ID_LENGTH);
		saltBA = reader.readBytes(OobProtocol.SALT_LENGTH);
		deviceNonce = reader.readBytes(OobProtocol.NONCE_LENGTH);
		publicKeyBA = reader.readBytes(OobProtocol.PUBLIC_KEY_BYTES);
		messageMac = reader.readBytesLeft();
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

	public byte[] getDeviceNonce() {
		return deviceNonce;
	}

	public byte[] getSalt() {
		return saltBA;
	}

	public byte[] getMessageMac() {
		return messageMac;
	}

	private byte[] calculateMac(OobAuthSession session) {
		DatagramWriter writer = new DatagramWriter();
		writer.write(MESSAGE_TYPE, 3);
		writer.writeBytes(oobPswdIdBA);
		writer.writeBytes(saltBA);
		writer.writeBytes(deviceNonce);		
		writer.writeBytes(publicKeyBA);
		return session.getMac(writer.toByteArray());
	}
}
