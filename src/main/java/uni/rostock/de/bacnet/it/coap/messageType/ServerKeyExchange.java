package uni.rostock.de.bacnet.it.coap.messageType;

import java.util.Arrays;

import org.eclipse.californium.elements.util.DatagramReader;
import org.eclipse.californium.elements.util.DatagramWriter;
import org.eclipse.californium.scandium.util.ByteArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uni.rostock.de.bacnet.it.coap.crypto.OobAuthSession;

public class ServerKeyExchange {

	private static final Logger LOG = LoggerFactory.getLogger(ServerKeyExchange.class);
	private static final int MESSAGE_TYPE = OobProtocol.SERVER_KEY_EXCHANGE;
	private final byte[] oobPswdIdBA;
	private final int deviceId;
	private final byte[] serverNonceBA;
	private final byte[] publicKeyBA;
	private final byte[] finalMessage;
	private final byte[] messageMac;

	public ServerKeyExchange(int deviceId, byte[] serverPubKey, OobAuthSession session) {
		if (session.getServerNonce() == null) {
			throw new NullPointerException("server nonce cannot be null");
		}
		if (session.getOobPswdId() == null) {
			throw new NullPointerException("oobPswdId cannot be null");
		}
		if (serverPubKey == null) {
			throw new NullPointerException("serverPubKey cannot be null");
		}
		serverNonceBA = session.getServerNonce();
		oobPswdIdBA = session.getOobPswdId();
		publicKeyBA = serverPubKey;
		this.deviceId = deviceId;
		DatagramWriter writer = new DatagramWriter();
		writer.write(MESSAGE_TYPE, 3);
		writer.writeBytes(oobPswdIdBA);
		writer.writeBytes(serverNonceBA);
		writer.write(deviceId, 32);
		writer.writeBytes(publicKeyBA);
		this.messageMac = calculateMac(session);
		writer.writeBytes(messageMac);
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
		oobPswdIdBA = reader.readBytes(OobProtocol.OOB_PSWD_ID_LENGTH);
		serverNonceBA = reader.readBytes(OobProtocol.NONCE_LENGTH);
		deviceId = reader.read(OobProtocol.DEVICE_ID_LENGTH);
		publicKeyBA = reader.readBytes(OobProtocol.PUBLIC_KEY_BYTES);
		messageMac = reader.readBytesLeft();
		LOG.info("server messsage successfully de-serailized");
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
		writer.writeBytes(oobPswdIdBA);
		writer.writeBytes(serverNonceBA);
		writer.writeBytes(session.getdeviceNonce());
		writer.write(deviceId, 32);
		writer.writeBytes(publicKeyBA);
		return session.getMac(writer.toByteArray());
	}

	public byte[] getMessageMac() {
		return messageMac;
	}
	
	public byte[] getServerNonce() {
		return serverNonceBA;
	}
}
