package uni.rostock.de.bacnet.it.coap.messageType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfirmAddDeviceRequest {

	private static final Logger LOG = LoggerFactory.getLogger(ConfirmAddDeviceRequest.class);
	private static final int MESSAGE_ID = OOBProtocol.CONFIRM_ADD_DEVICE_REQUEST.getValue();
	private final int sequenceId;
	private final byte[] pswdId;
	private int status;
	private byte[] finalMessage;

	public ConfirmAddDeviceRequest(int sequenceId, byte[] pswdId, int status) {
		this.sequenceId = sequenceId;
		this.pswdId = pswdId;
		this.status = status;
		generateBA();
	}

	private void generateBA() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			bos.write(MESSAGE_ID);
			bos.write(status);
			bos.write(ByteBuffer.allocate(Integer.BYTES).putInt(sequenceId).array());
			bos.write(pswdId);
		} catch (IOException e) {
			LOG.error(e.getMessage());
			e.printStackTrace();
		}

		this.finalMessage = bos.toByteArray();

	}

	public byte[] getBA() {
		return this.finalMessage;
	}

	public ConfirmAddDeviceRequest(byte[] finalMessage) {
		this.finalMessage = finalMessage;
		int count = 1;
		this.status = finalMessage[count];
		count += 1;
		byte[] sequenceIdBA = new byte[Integer.BYTES];
		System.arraycopy(finalMessage, count, sequenceIdBA, 0, Integer.BYTES);
		count += 4;
		this.sequenceId = ByteBuffer.wrap(sequenceIdBA).getInt();
		// 4 bytes are used to identify the OOB password
		this.pswdId = new byte[Integer.BYTES];
		System.arraycopy(finalMessage, count, pswdId, 0, Integer.BYTES);
	}

	public int getStatus() {
		return this.status;
	}

	public byte[] getPasswordId() {
		return this.pswdId;
	}

	public int getSequenceId() {
		return this.sequenceId;
	}
}
