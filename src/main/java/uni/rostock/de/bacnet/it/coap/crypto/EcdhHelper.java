package uni.rostock.de.bacnet.it.coap.crypto;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.DerivationParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.math.ec.rfc7748.X25519;
import org.eclipse.californium.scandium.util.ByteArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.upokecenter.cbor.CBORObject;

import uni.rostock.de.bacnet.it.coap.messageType.OOBProtocol;

public class EcdhHelper implements EcdhHelperTasks {

	private static final Logger LOG = LoggerFactory.getLogger(EcdhHelper.class);
	byte[] privateKeyBA = new byte[OOBProtocol.PRIVATE_KEY_BYTES.getValue()];
	byte[] publicKeyBA = new byte[OOBProtocol.PUBLIC_KEY_BYTES.getValue()];
	private byte[] sharedSecretBA = new byte[OOBProtocol.SECRET_KEY_BYTES.getValue()];
	byte[] macKeyBA = new byte[OOBProtocol.MAC_KEY_BYTES.getValue()];
	final static String COMMON_SALT = "out of band authentication";
	SecureRandom random;
	private final int curveType;
	private final String oobPswdKeyString;
	private byte[] oobPswdIdBA = new byte[OOBProtocol.OOB_PSWD_KEY_ID_LENGTH.getValue()];

	public EcdhHelper(int curveType, String oobPswdArray) {
		this.curveType = curveType;
		this.oobPswdKeyString = oobPswdArray;
		random = new SecureRandom();
		genereateAsymmetricKeys();
		deriveKeyFromPswd();
	}

	private void genereateAsymmetricKeys() {
		random.nextBytes(privateKeyBA);
		X25519.scalarMultBase(privateKeyBA, 0, publicKeyBA, 0);
	}

	public int getOOBPswd2Integer() {
		int value = Integer.valueOf(oobPswdKeyString, 2);
		return value;
	}

	private void deriveKeyFromPswd() {
		int oobPswdAsInt = getOOBPswd2Integer();
		/* idea adapted from sebastian unger oob protocol for DPWS */
		byte[] oobPswdAsBA = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }; /* ouch... */
		oobPswdAsBA[15] = (byte) (oobPswdAsInt & 0xFF);
		oobPswdAsBA[14] = (byte) ((oobPswdAsInt >> 8) & 0xFF);
		oobPswdAsBA[13] = (byte) ((oobPswdAsInt >> 16) & 0xFF);
		Digest digest = new SHA256Digest();
		HKDFBytesGenerator hkdf = new HKDFBytesGenerator(digest);
		CBORObject info = CBORObject.NewArray();
		info.Add(CBORObject.FromObject("passwordKey"));
		info.Add(OOBProtocol.OOB_PSWD_KEY_LENGTH);
		DerivationParameters param = new HKDFParameters(oobPswdAsBA, COMMON_SALT.getBytes(), info.EncodeToBytes());
		hkdf.init(param);
		hkdf.generateBytes(macKeyBA, 0, macKeyBA.length);
	}

	public int getCurveType() {
		return this.curveType;
	}

	@Override
	public byte[] getMac(byte[] data) {
		byte[] macData = null;
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			SecretKey macKey = new SecretKeySpec(macKeyBA, "HmacSHA256");
			mac.init(macKey);
			macData = mac.doFinal(data);
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			e.printStackTrace();
		}
		return macData;
	}

	@Override
	public byte[] getPubKeyBytes() {
		return this.publicKeyBA;
	}

	@Override
	public void computeSharedSecret(byte[] foreignPubKeyBA) {
		LOG.info("foreign public key bytes: " + ByteArrayUtils.toHex(foreignPubKeyBA));
		LOG.info("auth private key bytes: " + ByteArrayUtils.toHex(privateKeyBA));
		X25519.scalarMult(privateKeyBA, 0, foreignPubKeyBA, 0, sharedSecretBA, 0);
	}

	public byte[] getSharedSecret() {
		return sharedSecretBA;
	}

	public byte[] getOObPswdKeyIdentifier() {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			digest.update(oobPswdKeyString.getBytes());
			byte[] hash = digest.digest();
			for (int i = 0; i < 4; i++) {
				oobPswdIdBA[i] = hash[i];
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return oobPswdIdBA;
	}
}
