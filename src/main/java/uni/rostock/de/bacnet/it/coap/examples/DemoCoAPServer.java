package uni.rostock.de.bacnet.it.coap.examples;

import java.security.KeyPair;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite.KeyExchangeAlgorithm;

public class DemoCoAPServer {

	public static void main(String[] args) {
		CoapServer server = new CoapServer();
		server.add(new CoapResource("light") {
			@Override
			public void handlePOST(CoapExchange exchange) {
				KeyExchangeAlgorithm.PSK;
				System.out.println("request received");
			}
			
			@Override
			public void handleGET(CoapExchange exchnage) {
				System.out.println("get request received");
			}
		});
		server.start();
	}
}
