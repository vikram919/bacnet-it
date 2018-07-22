package uni.rostock.de.bacnet.it.coap.oobAuth;

public class OobProtocol {

	/* A byte is allocated to inform the type of Message id */

	public static final int ADD_DEVICE_REQUEST = 1;
	
	public static final int DEVICE_KEY_EXCHANGE = 2;

	public static final int SERVER_KEY_EXCHANGE = 3;
	
	public static final int OOB_STATUS = 5;

	/* Default as spcecified in IEEE 802.15.6-2012 */
	public static final int CURVE_secp256r1 = 1;

	public static final int X25519 = 2;

	public static final int PRIVATE_KEY_BYTES = 32;

	public static final int PUBLIC_KEY_BYTES = 32;

	public static final int SECRET_KEY_BYTES = 32;

	public static final int MAC_KEY_BYTES = 32;

	public static final int OOB_PSWD_KEY_LENGTH = 16;

	public static final int OOB_PSWD_ID_LENGTH = 8;

	public static final int DERIVED_KEYS_LENGTH = 48;
	
	public static final int NONCE_LENGTH = 8; // Bytes
	
	public static final int SALT_LENGTH = 8; // Bytes
	
	public static final int DEVICE_ID_LENGTH = 32; // bits

	/* Authentication success (random value feel free to change) */
	public static final int AUTHENTICATION_SUCCESS = 201; // try to put the value < 256
	/* Authentication failure (random value feel free to change) */
	public static final int AUTHENTICATION_FAILURE = 204;

}
