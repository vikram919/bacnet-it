package uni.rostock.de.bacnet.it.coap.messageType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uni.rostock.de.bacnet.it.coap.crypto.EcdhHelper;

public class Dh1Message {

	private static final Logger LOG = LoggerFactory.getLogger(Dh1Message.class);
	private final int messageId = OOBProtocol.DH1.getValue();
	private final byte[] oobPswdIdBA;
	private final byte[] publicKeyBA;
	private final byte[] macData;
	private final byte[] finalMessage;

	public Dh1Message(EcdhHelper ecdh_Helper) {

		this.oobPswdIdBA = ecdh_Helper.getOObPswdKeyIdentifier();
		this.publicKeyBA = ecdh_Helper.getPubKeyBytes();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			bos.write(messageId);
			bos.write(oobPswdIdBA);
			bos.write(publicKeyBA);
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.macData = ecdh_Helper.getMac(bos.toByteArray());
		this.finalMessage = serilaizeToBA();
		LOG.debug("message serilaized to BA");
	}

	public Dh1Message(byte[] finalBA) {
		this.finalMessage = finalBA;
		int count = 1;
		oobPswdIdBA = new byte[OOBProtocol.OOB_PSWD_KEY_LENGTH.getValue()];
		System.arraycopy(finalBA, count, oobPswdIdBA, 0, oobPswdIdBA.length);
		count += 4;
		this.publicKeyBA = new byte[OOBProtocol.PUBLIC_KEY_BYTES.getValue()];
		System.arraycopy(finalBA, count, publicKeyBA, 0, publicKeyBA.length);
		count += publicKeyBA.length;
		this.macData = new byte[finalBA.length - count];
		System.arraycopy(finalBA, count, macData, 0, macData.length);
		LOG.debug("message deserialized");
	}

	private byte[] serilaizeToBA() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			bos.write(messageId);
			bos.write(oobPswdIdBA);
			bos.write(publicKeyBA);
			bos.write(macData);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bos.toByteArray();
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
