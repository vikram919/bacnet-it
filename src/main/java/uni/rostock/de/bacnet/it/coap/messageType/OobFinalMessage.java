package uni.rostock.de.bacnet.it.coap.messageType;

import org.eclipse.californium.elements.util.DatagramReader;
import org.eclipse.californium.elements.util.DatagramWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uni.rostock.de.bacnet.it.coap.crypto.OobAuthSession;

public class OobFinalMessage {

	private static final Logger LOG = LoggerFactory.getLogger(OobFinalMessage.class.getCanonicalName());
	private static final int MESSAGE_TYPE = OOBProtocol.FINISH_MESSAGE.getValue();
	private final byte[] oobPswdIdBA;
	private final byte[] serverNonceBA;
	private final byte[] deviceNonceBA;
	private final byte[] finalMessageBA;
	private boolean isMacVerified = false;

	public OobFinalMessage(OobAuthSession session) {
		this.oobPswdIdBA = session.getOobPswdId();
		this.serverNonceBA = session.getServerNonce();
		this.deviceNonceBA = session.getdeviceNonce();
		DatagramWriter writer = new DatagramWriter();
		writer.write(MESSAGE_TYPE, 3);
		writer.writeBytes(oobPswdIdBA);
		writer.writeBytes(serverNonceBA);
		writer.writeBytes(deviceNonceBA);
		byte[] macData = calculateMac(session);
		writer.writeBytes(macData);
		this.finalMessageBA = writer.toByteArray();
	}

	public OobFinalMessage(OobAuthSession session, byte[] finalBA) {
		DatagramReader reader = new DatagramReader(finalBA);
		int messageType = reader.read(3);
		if (messageType != MESSAGE_TYPE) {
			LOG.debug("ServerKeyExchange authentication failed, wrong messageType received");
		}
		this.finalMessageBA = finalBA;
		oobPswdIdBA = reader.readBytes(OOBProtocol.OOB_PSWD_ID_LENGTH.getValue());
		serverNonceBA = reader.readBytes(OOBProtocol.NONCE_LENGTH.getValue());
		deviceNonceBA = reader.readBytes(OOBProtocol.NONCE_LENGTH.getValue());
		byte[] receviedMac = reader.readBytesLeft();
		byte[] calculatedMac = calculateMac(session);
		if (calculatedMac.equals(receviedMac)) {
			setMacVerified(true);
		} else {
			LOG.debug("ServerKeyExchange authentication failed, MAC verfication failed");
		}
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
