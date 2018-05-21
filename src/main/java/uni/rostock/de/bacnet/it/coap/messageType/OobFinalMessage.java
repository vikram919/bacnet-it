package uni.rostock.de.bacnet.it.coap.messageType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import uni.rostock.de.bacnet.it.coap.crypto.EcdhHelper;

public class OobFinalMessage {

	private final int messageId = OOBProtocol.FINISH_MESSAGE.getValue();
	private int deviceId;
	private int authId;
	private final byte[] message;
	private final byte[] macData;
	private byte[] finalBA;

	public OobFinalMessage(int deviceId, int authId, EcdhHelper ecdhHelper) {
		this.deviceId = deviceId;
		this.authId = authId;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			bos.write(messageId);
			bos.write(ByteBuffer.allocate(Integer.BYTES).putInt(deviceId).array());
			bos.write(ByteBuffer.allocate(Integer.BYTES).putInt(authId).array());
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.message = bos.toByteArray();
		this.macData = ecdhHelper.getMac(message);
		ByteArrayOutputStream finalMessage = new ByteArrayOutputStream();
		try {
			finalMessage.write(message);
			finalMessage.write(macData);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.finalBA = finalMessage.toByteArray();
	}

	public OobFinalMessage(byte[] finalBA) {
		int counter = 1;
		byte[] deviceIdBA = new byte[Integer.BYTES];
		System.arraycopy(finalBA, counter, deviceIdBA, 0, deviceIdBA.length);
		this.deviceId = ByteBuffer.wrap(deviceIdBA).getInt();
		counter += 4;
		byte[] authIdBA = new byte[Integer.BYTES];
		System.arraycopy(finalBA, counter, authIdBA, 0, authIdBA.length);
		this.authId = ByteBuffer.wrap(authIdBA).getInt();
		counter += 4;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			bos.write(ByteBuffer.allocate(Integer.BYTES).putInt(authId).array());
			bos.write(ByteBuffer.allocate(Integer.BYTES).putInt(deviceId).array());
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.message = bos.toByteArray();
		this.macData = new byte[finalBA.length - counter];
		System.arraycopy(finalBA, counter, macData, 0, macData.length);
	}

	public byte[] getBA() {
		return this.finalBA;
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

	public byte[] getMessage() {
		return this.message;
	}
}
