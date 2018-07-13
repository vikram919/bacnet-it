package uni.rostock.de.bacnet.it.coap.messageType;

public enum OOBProtocol {

	/* A byte is allocated to inform the type of Message id */

	ADD_DEVICE_REQUEST(1),
	
	DH1_MESSAGE(2),

	DH2_MESSAGE(3),

	FINISH_MESSAGE(4),

	/* Default as spcecified in IEEE 802.15.6-2012 */
	CURVE_secp256r1(1),

	X25519(2),

	PRIVATE_KEY_BYTES(32),

	PUBLIC_KEY_BYTES(32),

	SECRET_KEY_BYTES(32),

	MAC_KEY_BYTES(32),

	OOB_PSWD_KEY_LENGTH(16),

	OOB_PSWD_KEY_ID_LENGTH(4),

	DERIVED_KEYS_LENGTH(48),

	/* Authentication success (random value feel free to change) */
	AUTHENTICATION_SUCCESS(201), // try to put the value < 256
	/* Authentication failure (random value feel free to change) */
	AUTHENTICATION_FAILURE(204);

	private final int value;

	private OOBProtocol(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

}
