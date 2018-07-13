package uni.rostock.de.bacnet.it.coap.messageType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddDeviceRequest {

	private static final Logger LOG = LoggerFactory.getLogger(AddDeviceRequest.class);
	private static int MESSAGE_ID = OOBProtocol.ADD_DEVICE_REQUEST.getValue();
	/*
	 * Sequence number for identifying no. of devices the mobile has authenticated
	 */
	private final int sequenceId;
	/* bits in string representation */
	private final String bitKeyString;
	/* serialized byte array of the AddDeviceRequest */
	private byte[] finalMessage;

	public AddDeviceRequest(int sequenceId, String bitKeyString) {
		this.sequenceId = sequenceId;
		this.bitKeyString = bitKeyString;
		serializeToByteArray();
	}

	public void serializeToByteArray() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		bos.write(MESSAGE_ID);
		try {
			bos.write(ByteBuffer.allocate(Integer.BYTES).putInt(sequenceId).array());
			bos.write(bitKeyString.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			LOG.error(e.getMessage());
			e.printStackTrace();
		}
		this.finalMessage = bos.toByteArray();
	}

	/*
	 * builds a WritePropertyRequest including the byte array as OctetString.
	 */
	public byte[] getBA() {
		return this.finalMessage;
	}

	public AddDeviceRequest(byte[] finalMessage) {
		this.finalMessage = finalMessage;
		int count = 1;
		byte[] sequenceBA = new byte[Integer.BYTES];
		System.arraycopy(finalMessage, count, sequenceBA, 0, Integer.BYTES);
		this.sequenceId = ByteBuffer.wrap(sequenceBA).getInt();
		count += 4;
		byte[] bitKeyStringBA = new byte[finalMessage.length - count];
		System.arraycopy(finalMessage, count, bitKeyStringBA, 0, bitKeyStringBA.length);
		this.bitKeyString = new String(bitKeyStringBA);
	}

	public int getSequenceId() {
		return this.sequenceId;
	}

	public String getBitKeyString() {
		return this.bitKeyString;
	}
}
