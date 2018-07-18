package uni.rostock.de.bacnet.it.coap.crypto;

import java.util.Arrays;

import org.bouncycastle.crypto.DerivationParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.eclipse.californium.elements.util.DatagramWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.upokecenter.cbor.CBORObject;

import uni.rostock.de.bacnet.it.coap.messageType.DeviceKeyExchange;
import uni.rostock.de.bacnet.it.coap.messageType.OobProtocol;
import uni.rostock.de.bacnet.it.coap.messageType.ServerKeyExchange;

/**
 * class OobAuthSession is implemented to have separate auth session between
 * devices, where as ecdhHelper class will be only one supporting cryptographic
 * operations.
 * 
 * @author vik
 *
 */
public class OobAuthSession {

	private static Logger LOG = LoggerFactory.getLogger(OobAuthSession.class.getCanonicalName());
	private final EcdhHelper ecdhHelper;
	private final String oobPswdString;
	private byte[] oobPswdKey = null;
	private byte[] oobPswdSalt = null;
	private byte[] deviceNonce = null;
	private byte[] serverNonce = null;

	public OobAuthSession(EcdhHelper ecdhHelper, String oobPswdString) {
		this.ecdhHelper = ecdhHelper;
		this.oobPswdString = oobPswdString;
	}

	public void setSalt(byte[] oobPswdSalt) {
		this.oobPswdSalt = oobPswdSalt;
	}

	public void setClientNonce(byte[] clientNonce) {
		this.deviceNonce = clientNonce;
	}

	public void setServerNonce(byte[] serverNonce) {
		this.serverNonce = serverNonce;
	}

	public byte[] getServerNonce() {
		return this.serverNonce;
	}

	public byte[] getdeviceNonce() {
		return this.deviceNonce;
	}

	public byte[] getOobPswdSalt() {
		return this.oobPswdSalt;
	}

	public void deriveOobPswdKey(byte[] salt) {
		if (salt == null) {
			throw new NullPointerException("salt cannot be null");
		}
		int oobPswdAsInt = getOOBPswdString2Int();
		/* idea adapted from sebastian unger oob protocol for DPWS */
		byte[] oobPswdAsBA = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }; /* ouch... */
		oobPswdAsBA[15] = (byte) (oobPswdAsInt & 0xFF);
		oobPswdAsBA[14] = (byte) ((oobPswdAsInt >> 8) & 0xFF);
		oobPswdAsBA[13] = (byte) ((oobPswdAsInt >> 16) & 0xFF);
		Digest digest = new SHA256Digest();
		HKDFBytesGenerator hkdf = new HKDFBytesGenerator(digest);
		CBORObject info = CBORObject.NewArray();
		info.Add(CBORObject.FromObject("out of band authentication password"));
		info.Add(OobProtocol.OOB_PSWD_KEY_LENGTH);
		DerivationParameters param = new HKDFParameters(oobPswdAsBA, salt, info.EncodeToBytes());
		hkdf.init(param);
		this.oobPswdKey = new byte[OobProtocol.OOB_PSWD_KEY_LENGTH];
		hkdf.generateBytes(oobPswdKey, 0, oobPswdKey.length);
	}

	private int getOOBPswdString2Int() {
		int value = Integer.valueOf(oobPswdString, 2);
		return value;
	}

	public byte[] getOobPswdId() {
		return ecdhHelper.getOobPswdId(oobPswdString);
	}

	public byte[] getMac(byte[] data) {
		if (oobPswdKey == null) {
			throw new NullPointerException(
					"oobPswdKey cannot be null, consider calling deriveOobKey method for current OobAuthSession");
		}
		return ecdhHelper.getMac(oobPswdKey, data);
	}

	public boolean isDeviceKeyExchangeMessageAuthenticated(DeviceKeyExchange deviceKeyExchange) {
		deriveOobPswdKey(deviceKeyExchange.getSalt());
		boolean val = Arrays.equals(calculateDeviceKeyExchangeMac(deviceKeyExchange),
				deviceKeyExchange.getMessageMac());
		LOG.debug("mac verification result: " + val);
		return val;
	}

	private byte[] calculateDeviceKeyExchangeMac(DeviceKeyExchange deviceKeyExchange) {
		DatagramWriter writer = new DatagramWriter();
		writer.write(OobProtocol.DEVICE_KEY_EXCHANGE, 3);
		writer.writeBytes(deviceKeyExchange.getOobPswdIdBA());
		writer.writeBytes(deviceKeyExchange.getSalt());
		writer.writeBytes(deviceKeyExchange.getDeviceNonce());
		writer.writeBytes(deviceKeyExchange.getPublicKeyBA());
		return getMac(writer.toByteArray());
	}

	public boolean isServerKeyExchangeMessageAuthenticated(ServerKeyExchange serverKeyExchange) {
		setServerNonce(serverKeyExchange.getServerNonce());
		boolean val = Arrays.equals(caluclateServerKeyExchangeMac(serverKeyExchange),
				serverKeyExchange.getMessageMac());
		LOG.info("mac verification result: " + val);
		return val;
	}

	private byte[] caluclateServerKeyExchangeMac(ServerKeyExchange serverKeyExchange) {
		DatagramWriter writer = new DatagramWriter();
		writer.write(OobProtocol.SERVER_KEY_EXCHANGE, 3);
		writer.writeBytes(serverKeyExchange.getOobPswdIdBA());
		writer.writeBytes(serverKeyExchange.getServerNonce());
		writer.writeBytes(getdeviceNonce());
		writer.write(serverKeyExchange.getDeviceId(), 32);
		writer.writeBytes(serverKeyExchange.getPublicKeyBA());
		return getMac(writer.toByteArray());
	}
}
