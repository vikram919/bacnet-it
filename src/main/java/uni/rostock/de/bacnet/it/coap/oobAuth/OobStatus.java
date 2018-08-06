package uni.rostock.de.bacnet.it.coap.oobAuth;

import org.eclipse.californium.elements.util.DatagramReader;
import org.eclipse.californium.elements.util.DatagramWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OobStatus {
	private static final Logger LOG = LoggerFactory.getLogger(AddDeviceRequest.class);
	private static final int MESSAGE_TYPE = OobProtocol.OOB_STATUS;

	/* authentication status of the device */
	private int oobStatus = 0;

	/* serialized byte array of the AddDeviceRequest */
	private byte[] finalMessage;

	public OobStatus(byte[] oobPswdId, boolean oobStatus) {
		this.oobStatus = (oobStatus) ? 1:0;
		DatagramWriter writer = new DatagramWriter();
		writer.write(MESSAGE_TYPE, 3);
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
		this.oobStatus = reader.read(1);
	}
	
	public boolean getOobStatus() {
		return (this.oobStatus==1)?true:false;
	}

	public byte[] getBA() {
		return this.finalMessage;
	}
}
