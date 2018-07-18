package uni.rostock.de.bacnet.it.coap.messageType;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.californium.scandium.util.ByteArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uni.rostock.de.bacnet.it.coap.crypto.EcdhHelper;
import uni.rostock.de.bacnet.it.coap.crypto.OobAuthSession;

public class OobSessionsStore {

	private static final Logger LOG = LoggerFactory.getLogger(OobSessionsStore.class.getCanonicalName());
	private static OobSessionsStore object;
	private EcdhHelper ecdhHelper = new EcdhHelper(0);
	private Map<String, OobAuthSession> deviceSessionsMap = new HashMap<>();

	private OobSessionsStore() {

	}

	public static OobSessionsStore getInstance() {
		if (object == null) {
			object = new OobSessionsStore();
		}
		return object;
	}

	public void addDeviceoobPswd(String oobPswdString) {
		byte[] oobPswdId = ecdhHelper.getOobPswdId(oobPswdString);
		if (deviceSessionsMap.containsKey(new String(oobPswdId, StandardCharsets.UTF_8))) {
			LOG.debug("ignoring storing password, cause given password already exists in OobSessionsStore");
		} else {
			OobAuthSession session = new OobAuthSession(ecdhHelper, oobPswdString);
			deviceSessionsMap.put(new String(oobPswdId, StandardCharsets.UTF_8), session);
			LOG.info(ByteArrayUtils.toHex(oobPswdId));
		}
	}

	public boolean hasOobPswdId(byte[] oobPswdId) {
		return deviceSessionsMap.containsKey(new String(oobPswdId, StandardCharsets.UTF_8));
	}

	public OobAuthSession getAuthSession(byte[] oobPswdId) {
		return deviceSessionsMap.get(new String(oobPswdId, StandardCharsets.UTF_8));
	}

	public byte[] getPubKey() {
		return ecdhHelper.getPubKeyBytes();
	}

	public byte[] getServerNonce() {
		return ecdhHelper.getRandomBytes(8);
	}

	public byte[] getClientNonce() {
		return ecdhHelper.getRandomBytes(8);
	}
}
