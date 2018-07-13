package uni.rostock.de.bacnet.it.coap.messageType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uni.rostock.de.bacnet.it.coap.crypto.EcdhHelper;

public class ServerKeyExchange {
	
	private static final Logger LOG = LoggerFactory.getLogger(ServerKeyExchange.class);
	private static final int MESSAGE_ID = OOBProtocol.DH2_MESSAGE.getValue();
	private final byte[] oobPswdIdBA;
	private final int authId;
	private final int deviceId;
	private final byte[] publicKeyBA;
	private final byte[] macData;
	private final byte[] finalMessage;

	public ServerKeyExchange(int authId, int deviceId, EcdhHelper ecdhHelper) {
		this.oobPswdIdBA = ecdhHelper.getOObPswdKeyIdentifier();
		this.publicKeyBA = ecdhHelper.getPubKeyBytes();
		this.authId = authId;
		this.deviceId = deviceId;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			bos.write(MESSAGE_ID);
			bos.write(oobPswdIdBA);
			bos.write(ByteBuffer.allocate(Integer.BYTES).putInt(authId).array());
			bos.write(ByteBuffer.allocate(Integer.BYTES).putInt(deviceId).array());
			bos.write(publicKeyBA);
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.macData = ecdhHelper.getMac(bos.toByteArray());
		this.finalMessage = serilaizeToBA();
		LOG.debug("message serialized");
	}

	public ServerKeyExchange(byte[] finalBA) {
		this.finalMessage = finalBA;
		int count = 1;
		oobPswdIdBA = new byte[OOBProtocol.OOB_PSWD_KEY_LENGTH.getValue()];
		System.arraycopy(finalBA, count, oobPswdIdBA, 0, oobPswdIdBA.length);
		count += 4;
		byte[] authIdBA = new byte[Integer.BYTES];
		System.arraycopy(finalBA, count, authIdBA, 0, authIdBA.length);
		this.authId = ByteBuffer.wrap(authIdBA).getInt();
		count+=4;
		byte[] deviceIdBA = new byte[Integer.BYTES];
		System.arraycopy(finalBA, count, deviceIdBA, 0, deviceIdBA.length);
		this.deviceId = ByteBuffer.wrap(deviceIdBA).getInt();
		count+=4;
		publicKeyBA = new byte[OOBProtocol.PUBLIC_KEY_BYTES.getValue()];
		System.arraycopy(finalBA, count, publicKeyBA, 0, publicKeyBA.length);
		count += publicKeyBA.length;
		this.macData = new byte[finalBA.length - count];
		System.arraycopy(finalBA, count, macData, 0, macData.length);
	}

	private byte[] serilaizeToBA() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			bos.write(MESSAGE_ID);
			bos.write(oobPswdIdBA);
			bos.write(ByteBuffer.allocate(Integer.BYTES).putInt(authId).array());
			bos.write(ByteBuffer.allocate(Integer.BYTES).putInt(deviceId).array());
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

	public int getDeviceId() {
		return this.deviceId;
	}

	public int getAuthId() {
		return this.authId;
	}
	
	public byte[] getMacData() {
		return this.macData;
	}
}
