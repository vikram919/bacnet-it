package uni.rostock.de.bacnet.it.coap.messageType;

import org.eclipse.californium.elements.util.DatagramReader;
import org.eclipse.californium.elements.util.DatagramWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uni.rostock.de.bacnet.it.coap.crypto.OobAuthSession;

public class OobFinalMessage {

	private static final Logger LOG = LoggerFactory.getLogger(OobFinalMessage.class.getCanonicalName());
	private static final int MESSAGE_TYPE = OobProtocol.FINISH_MESSAGE;
	private final byte[] oobPswdIdBA;
	private final int deviceId;
	private final byte[] serverNonceBA;
	private final byte[] deviceNonceBA;
	private final byte[] messageMac;
	private final byte[] finalMessageBA;
	private boolean isMacVerified = false;

	public OobFinalMessage(OobAuthSession session) {
		if (session.getOobPswdId() == null) {
			throw new NullPointerException("password id cannot be null");
		}
		this.oobPswdIdBA = session.getOobPswdId();
		if (session.getDeviceId() == null) {
			throw new NullPointerException("deviceId cannot be null");
		}
		deviceId = session.getDeviceId();
		if (session.getServerNonce() == null) {
			throw new NullPointerException("server nonce cannot be null");
		}
		serverNonceBA = session.getServerNonce();
		if (session.getdeviceNonce() == null) {
			throw new NullPointerException("device nonce cannot be null");
		}
		deviceNonceBA = session.getdeviceNonce();
		DatagramWriter writer = new DatagramWriter();
		writer.write(MESSAGE_TYPE, 3);
		writer.writeBytes(oobPswdIdBA);
		writer.write(deviceId, 32);
		writer.writeBytes(serverNonceBA);
		writer.writeBytes(deviceNonceBA);
		messageMac = calculateMac(session);
		writer.writeBytes(messageMac);
		this.finalMessageBA = writer.toByteArray();
	}

	public OobFinalMessage(OobAuthSession session, byte[] finalBA) {
		DatagramReader reader = new DatagramReader(finalBA);
		int messageType = reader.read(3);
		if (messageType != MESSAGE_TYPE) {
			LOG.debug("oobFinalMessage authentication failed, wrong messageType received");
		}
		this.finalMessageBA = finalBA;
		oobPswdIdBA = reader.readBytes(OobProtocol.OOB_PSWD_ID_LENGTH);
		deviceId = reader.read(32);
		serverNonceBA = reader.readBytes(OobProtocol.NONCE_LENGTH);
		deviceNonceBA = reader.readBytes(OobProtocol.NONCE_LENGTH);
		messageMac = reader.readBytesLeft();
		LOG.info("OobFinalMessage successfully de-serialized");
	}

	public byte[] getBA() {
		return this.finalMessageBA;
	}

	public byte[] getServerNonce() {
		return this.serverNonceBA;
	}

	public byte[] getDeviceNonce() {
		return this.deviceNonceBA;
	}

	public byte[] getOobPswdIdBA() {
		return this.oobPswdIdBA;
	}

	private byte[] calculateMac(OobAuthSession session) {
		DatagramWriter writer = new DatagramWriter();
		writer.write(MESSAGE_TYPE, 3);
		writer.writeBytes(oobPswdIdBA);
		writer.writeBytes(session.getServerNonce());
		writer.writeBytes(session.getdeviceNonce());
		return session.getMac(writer.toByteArray());
	}

	public boolean isMacVerified() {
		return isMacVerified;
	}

	public void setMacVerified(boolean isMacVerified) {
		this.isMacVerified = isMacVerified;
	}
}
