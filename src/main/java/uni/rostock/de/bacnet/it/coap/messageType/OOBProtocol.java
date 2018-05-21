package uni.rostock.de.bacnet.it.coap.messageType;

public enum OOBProtocol {

	/* A byte is allocated to inform the type of Message id */

	ADD_DEVICE_REQUEST(1),

	CONFIRM_ADD_DEVICE_REQUEST(2),

	DH1(3),

	DH2(4),

	FINISH_MESSAGE(5),

	/* Default as spcecified in IEEE 802.15.6-2012 */
	CURVE_secp256r1(1),

	X25519(2),

	PRIVATE_KEY_BYTES(32),

	PUBLIC_KEY_BYTES(32),

	SECRET_KEY_BYTES(32),

	MAC_KEY_BYTES(32),

	OOB_PSWD_KEY_LENGTH(16),
	
	OOB_PSWD_KEY_ID_LENGTH(4),

	DERIVED_KEYS_LENGTH(48);

	private final int value;

	private OOBProtocol(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

}