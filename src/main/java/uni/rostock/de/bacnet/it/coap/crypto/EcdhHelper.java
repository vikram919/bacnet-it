package uni.rostock.de.bacnet.it.coap.crypto;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.math.ec.rfc7748.X25519;
import org.eclipse.californium.scandium.util.ByteArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uni.rostock.de.bacnet.it.coap.oobAuth.OobProtocol;

public class EcdhHelper implements EcdhHelperTasks {

	private static final Logger LOG = LoggerFactory.getLogger(EcdhHelper.class);
	private byte[] privateKeyBA = new byte[OobProtocol.PRIVATE_KEY_BYTES];
	private byte[] publicKeyBA = new byte[OobProtocol.PUBLIC_KEY_BYTES];
	private final SecureRandom random;

	private final int curveType;

	public EcdhHelper(int curveType) {
		this.curveType = curveType;
		random = new SecureRandom();
		genereateAsymmetricKeys();
	}

	private void genereateAsymmetricKeys() {
		random.nextBytes(privateKeyBA);
		X25519.scalarMultBase(privateKeyBA, 0, publicKeyBA, 0);
	}

	public int getCurveType() {
		return this.curveType;
	}

	@Override
	public byte[] getMac(byte[] oobPswdKey, byte[] data) {
		if (oobPswdKey == null) {
			throw new NullPointerException("oob password key cannot be null");
		}
		if (data == null) {
			throw new NullPointerException("data to be maced cannot be null");
		}
		byte[] macData = null;
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			SecretKey macKey = new SecretKeySpec(oobPswdKey, "HmacSHA256");
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
	public byte[] computeSharedSecret(byte[] foreignPubKeyBA) {
		byte[] sharedSecretBA = new byte[OobProtocol.SECRET_KEY_BYTES];
		X25519.scalarMult(privateKeyBA, 0, foreignPubKeyBA, 0, sharedSecretBA, 0);
		return sharedSecretBA;
	}

	@Override
	public byte[] getRandomBytes(int bytes) {
		byte[] randomBytes = new byte[bytes];
		random.nextBytes(randomBytes);
		return randomBytes;
	}

	@Override
	public byte[] getOobPswdId(String oobPswdString) {
		byte[] oobPswdIdBA = new byte[OobProtocol.OOB_PSWD_ID_LENGTH];
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
}
