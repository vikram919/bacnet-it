package uni.rostock.de.bacnet.it.coap.crypto;

public interface EcdhHelperTasks {

	public byte[] getMac(byte[] data);

	public byte[] getPubKeyBytes();

	public void computeSharedSecret(byte[] foreignPubKeyBA);
	
	public byte[] getSalt();

}
