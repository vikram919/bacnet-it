package uni.rostock.de.bacnet.it.coap.messageType;

import java.nio.charset.StandardCharsets;

import org.eclipse.californium.elements.util.DatagramReader;
import org.eclipse.californium.elements.util.DatagramWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddDeviceRequest {

	private static final Logger LOG = LoggerFactory.getLogger(AddDeviceRequest.class);
	private static final int MESSAGE_TYPE = OOBProtocol.ADD_DEVICE_REQUEST.getValue();
	/*
	 * Sequence number for identifying no. of devices the mobile has authenticated
	 */
	private final short sequenceId;
	/* bits in string representation */
	private final String bitKeyString;
	/* serialized byte array of the AddDeviceRequest */
	private byte[] finalMessage;

	public AddDeviceRequest(short sequenceId, String bitKeyString) {
		this.sequenceId = sequenceId;
		this.bitKeyString = bitKeyString;
		DatagramWriter writer = new DatagramWriter();
		writer.write(MESSAGE_TYPE, 3);
		writer.write(sequenceId, 16);
		writer.writeBytes(bitKeyString.getBytes(StandardCharsets.UTF_8));
		finalMessage = writer.toByteArray();
		LOG.debug("AddDeviceRequest serialized to byte array");
	}

	public AddDeviceRequest(byte[] finalMessage) {
		DatagramReader reader = new DatagramReader(finalMessage);
		int messageType = reader.read(3);
		if (messageType != MESSAGE_TYPE) {
			LOG.info("AddDeviceRequest failed, wrong message type, expected {} but received {}", MESSAGE_TYPE,
					messageType);
		}
		this.finalMessage = finalMessage;
		this.sequenceId = (short) reader.read(16);
		this.bitKeyString = new String(reader.readBytesLeft());
	}

	public int getSequenceId() {
		return this.sequenceId;
	}

	public String getBitKeyString() {
		return this.bitKeyString;
	}

	public byte[] getBA() {
		return this.finalMessage;
	}
}
