package uni.rostock.de.bacnet.it.coap.oobAuth;

import java.nio.charset.StandardCharsets;

import org.eclipse.californium.elements.util.DatagramReader;
import org.eclipse.californium.elements.util.DatagramWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OobFinal {

	private static final Logger LOG = LoggerFactory.getLogger(OobFinal.class.getCanonicalName());
	public static final int MESSAGE_TYPE = OobProtocol.OOB_FINAL;
	public static final byte[] APPLICATION_MESSAGE = "Hello, this is device".getBytes(StandardCharsets.UTF_8);
	public final byte[] sessionIdentifier;
	public final byte[] messageMac;
	private final byte[] finalBA;

	public OobFinal(OobAuthSession session) {
		sessionIdentifier = session.getOobPswdId();
		DatagramWriter writer = new DatagramWriter();
		writer.write(MESSAGE_TYPE, 3);
		writer.writeBytes(sessionIdentifier);
		messageMac = getMessageMac(session);
		writer.writeBytes(messageMac);
		finalBA = writer.toByteArray();
		LOG.info("OobFinal message serialized");
	}
	
	public OobFinal(byte[] finalBA) {
		this.finalBA = finalBA;
		if (finalBA == null) {
			throw new NullPointerException("finalBA cannot be null");
		}
		DatagramReader reader = new DatagramReader(finalBA);
		int messageType = reader.read(3);
		if (messageType != MESSAGE_TYPE) {
			LOG.info("OObFinal message wrong messagetype received expected {} but received {1}",
					MESSAGE_TYPE, messageType);
		}
		sessionIdentifier = reader.readBytes(OobProtocol.OOB_PSWD_ID_LENGTH);
		messageMac = reader.readBytesLeft();
		LOG.info("OobFinal message deserialzed");
	}

	public byte[] getMessageMac(OobAuthSession session) {
		DatagramWriter writer = new DatagramWriter();
		writer.write(MESSAGE_TYPE, 3);
		writer.writeBytes(sessionIdentifier);
		writer.writeBytes(APPLICATION_MESSAGE);
		return session.getMac(writer.toByteArray());
	}
	
	public byte[] getBA() {
		return this.finalBA;
	}
	
	public byte[] getOobPswdId() {
		return this.sessionIdentifier;
	}
}
