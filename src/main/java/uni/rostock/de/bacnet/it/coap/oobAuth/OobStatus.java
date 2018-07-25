package uni.rostock.de.bacnet.it.coap.oobAuth;

import org.eclipse.californium.elements.util.DatagramReader;
import org.eclipse.californium.elements.util.DatagramWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OobStatus {
	private static final Logger LOG = LoggerFactory.getLogger(AddDeviceRequest.class);
	private static final int MESSAGE_TYPE = OobProtocol.OOB_STATUS;
	private final byte[] oobPswdId;

	/* authentication status of the device */
	private final int oobStatus;

	/* serialized byte array of the AddDeviceRequest */
	private byte[] finalMessage;

	public OobStatus(byte[] oobPswdId, boolean oobStatus) {
		this.oobPswdId = oobPswdId;
		if (oobStatus) {
			this.oobStatus = 1;
		} else {
			this.oobStatus = 0;
		}
		DatagramWriter writer = new DatagramWriter();
		writer.write(MESSAGE_TYPE, 3);
		writer.writeBytes(oobPswdId);
		writer.write(this.oobStatus, 1);
		finalMessage = writer.toByteArray();
		LOG.debug("OobStatus serialized to byte array");
	}

	public OobStatus(byte[] finalMessage) {
		DatagramReader reader = new DatagramReader(finalMessage);
		int messageType = reader.read(3);
		if (messageType != MESSAGE_TYPE) {
			LOG.info("OobStatus wrong message type received, expected {} but received {}", MESSAGE_TYPE, messageType);
		}
		this.finalMessage = finalMessage;
		this.oobPswdId = reader.readBytes(OobProtocol.OOB_PSWD_ID_LENGTH);
		this.oobStatus = reader.read(1);
	}

	public byte[] getOobPswdId() {
		return this.oobPswdId;
	}

	public boolean getOobStatus() {
		if (this.oobStatus == 1) {
			return true;
		} else {
			return false;
		}
	}

	public byte[] getBA() {
		return this.finalMessage;
	}
}
