package uni.rostock.de.bacnet.it.coap.messageType;

import org.eclipse.californium.elements.util.DatagramReader;
import org.eclipse.californium.elements.util.DatagramWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uni.rostock.de.bacnet.it.coap.crypto.EcdhHelper;

public class DeviceKeyExchange {

	private static final Logger LOG = LoggerFactory.getLogger(DeviceKeyExchange.class);
	private static final int MESSAGE_TYPE = OOBProtocol.DH1_MESSAGE.getValue();
	private final byte[] oobPswdIdBA;
	private final byte[] saltBA;
	private final byte[] publicKeyBA;
	private final byte[] macData;
	private final byte[] finalMessage;

	public DeviceKeyExchange(EcdhHelper ecdhHelper) {
		oobPswdIdBA = ecdhHelper.getOObPswdKeyIdentifier();
		publicKeyBA = ecdhHelper.getPubKeyBytes();
		saltBA = ecdhHelper.getSalt();
		DatagramWriter writer = new DatagramWriter();
		writer.write(MESSAGE_TYPE, 3);
		writer.writeBytes(oobPswdIdBA);
		writer.writeBytes(saltBA);
		writer.writeBytes(publicKeyBA);
		macData = ecdhHelper.getMac(writer.toByteArray());
		writer.writeBytes(macData);
		finalMessage = writer.toByteArray();
		LOG.debug("message serilaized to BA");
	}

	public DeviceKeyExchange(byte[] finalBA) {
		DatagramReader reader = new DatagramReader(finalBA);
		int messageType = reader.read(3);
		if (messageType != MESSAGE_TYPE) {
			// throw received unknown message
		}
		finalMessage = finalBA;
		oobPswdIdBA = reader.readBytes(OOBProtocol.OOB_PSWD_KEY_LENGTH.getValue());
		saltBA = reader.readBytes(8);
		publicKeyBA = reader.readBytes(OOBProtocol.PUBLIC_KEY_BYTES.getValue());
		macData = reader.readBytesLeft();
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

	public byte[] getMacData() {
		return this.macData;
	}
}
