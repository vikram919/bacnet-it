package uni.rostock.de.bacnet.it.coap.crypto;

public interface EcdhHelperTasks {

	public byte[] getMac(byte[] macKey, byte[] data);

	public byte[] getPubKeyBytes();

	public byte[] computeSharedSecret(byte[] foreignPubKeyBA);

	public byte[] getRandomBytes(int bytes);
	
	public byte[] getOobPswdId(String oobPswdString);

}
