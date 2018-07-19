package uni.rostock.de.bacnet.it.coap.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uni.rostock.de.bacnet.it.coap.crypto.OobAuthClient;

public class OobTestClient {
	private static Logger LOG = LoggerFactory.getLogger(OobTestClient.class.getCanonicalName());
	private static String OOB_PSWD_STRING = "10101110010101101011";

	public static void main(String[] args) {
		OobAuthClient oobClient = new OobAuthClient(OOB_PSWD_STRING, "coap://localhost:5683/authentication");
		oobClient.startHandShake();
	}

}
