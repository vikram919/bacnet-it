package uni.rostock.de.bacnet.it.coap.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.bouncycastle.crypto.DerivationParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;

import com.upokecenter.cbor.CBORObject;

import uni.rostock.de.bacnet.it.coap.messageType.OOBProtocol;

/**
 * class OobAuthSession is implemented to have separate auth session between
 * devices, where as ecdhHelper class will be only one supporting cryptographic
 * operations.
 * 
 * @author vik
 *
 */
public class OobAuthSession {

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

	public byte[] deriveOobPswdKey(byte[] salt) {
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
		info.Add(OOBProtocol.OOB_PSWD_KEY_LENGTH);
		DerivationParameters param = new HKDFParameters(oobPswdAsBA, salt, info.EncodeToBytes());
		hkdf.init(param);
		byte[] oobPswdKey = new byte[OOBProtocol.OOB_PSWD_KEY_LENGTH.getValue()];
		hkdf.generateBytes(oobPswdKey, 0, oobPswdKey.length);
		return oobPswdKey;
	}

	private int getOOBPswdString2Int() {
		int value = Integer.valueOf(oobPswdString, 2);
		return value;
	}

	public byte[] getOobPswdId() {
		byte[] oobPswdIdBA = new byte[OOBProtocol.OOB_PSWD_ID_LENGTH.getValue()];
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			digest.update(oobPswdString.getBytes());
			byte[] hash = digest.digest();
			for (int i = 0; i < 8; i++) {
				oobPswdIdBA[i] = hash[i];
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return oobPswdIdBA;
	}

	public byte[] getMac(byte[] data) {
		if (oobPswdKey == null) {
			throw new NullPointerException(
					"oobPswdKey cannot be null, consider calling deriveOobKey method for current OobAuthSession");
		}
		return ecdhHelper.getMac(oobPswdKey, data);
	}
}
