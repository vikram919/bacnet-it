package uni.rostock.de.bacnet.it.coap.messageType;

import org.eclipse.californium.elements.util.DatagramReader;
import org.eclipse.californium.elements.util.DatagramWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uni.rostock.de.bacnet.it.coap.crypto.EcdhHelper;

public class ServerKeyExchange {

	private static final Logger LOG = LoggerFactory.getLogger(ServerKeyExchange.class);
	private static final int MESSAGE_TYPE = OOBProtocol.SERVER_KEY_EXCHANGE.getValue();
	private final byte[] serverNonceBA;
	private final byte[] oobPswdIdBA;
	private final int deviceId;
	private final byte[] publicKeyBA;
	private final byte[] finalMessage;

	public ServerKeyExchange(int authId, int deviceId, EcdhHelper ecdhHelper) {
		serverNonceBA = ecdhHelper.getSalt();
		oobPswdIdBA = ecdhHelper.getOObPswdKeyIdentifier();
		publicKeyBA = ecdhHelper.getPubKeyBytes();
		this.deviceId = deviceId;
		DatagramWriter writer = new DatagramWriter();
		writer.write(MESSAGE_TYPE, 3);
		writer.writeBytes(serverNonceBA);
		writer.writeBytes(oobPswdIdBA);
		writer.write(deviceId, 32);
		writer.writeBytes(publicKeyBA);
		byte[] macData = calculateMac(ecdhHelper);
		writer.writeBytes(macData);
		this.finalMessage = writer.toByteArray();
		LOG.debug("ServerKeyExchange message serialized");
	}

	public ServerKeyExchange(EcdhHelper ecdhHelper, byte[] finalBA) {
		DatagramReader reader = new DatagramReader(finalBA);
		int messageType = reader.read(3);
		if (messageType != MESSAGE_TYPE) {
			LOG.debug("ServerKeyExchange authentication failed, wrong messageType received");
		}
		this.finalMessage = finalBA;
		serverNonceBA = reader.readBytes(OOBProtocol.NONCE_LENGTH.getValue());
		oobPswdIdBA = reader.readBytes(OOBProtocol.OOB_PSWD_KEY_ID_LENGTH.getValue());
		deviceId = reader.read(OOBProtocol.DEVICE_ID_LENGTH.getValue());
		publicKeyBA = new byte[OOBProtocol.PUBLIC_KEY_BYTES.getValue()];
		byte[] receviedMac = reader.readBytesLeft();
		byte[] calculatedMac = calculateMac(ecdhHelper);
		if (!calculatedMac.equals(receviedMac)) {
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

	private byte[] calculateMac(EcdhHelper ecdhHelper) {
		DatagramWriter writer = new DatagramWriter();
		writer.write(MESSAGE_TYPE, 3);
		writer.writeBytes(serverNonceBA);
		writer.writeBytes(oobPswdIdBA);
		writer.write(deviceId, 32);
		writer.writeBytes(publicKeyBA);
		return ecdhHelper.getMac(writer.toByteArray());
	}
}
