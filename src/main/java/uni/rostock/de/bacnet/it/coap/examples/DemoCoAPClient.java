package uni.rostock.de.bacnet.it.coap.examples;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.californium.core.coap.Request;

public class DemoCoAPClient {

	public static void main(String[] args) {
		
		CoapClient client = new CoapClient("coap://localhost:5683/light");
//		client.putIfNoneMatch("hello", MediaTypeRegistry.TEXT_PLAIN);
		Request request = new Request(Code.GET);
		request.getOptions().setIfNoneMatch(true);
		// etag of last received data
		byte[] etag = "dummy tag".getBytes();
		
		request.getOptions().addETag(etag);
		CoapResponse response = client.advanced(request);
		if(response!=null) {
			System.out.println(response.getCode());
			System.out.println(new String(response.getOptions().getETags().get(0)));
		}

	}

}
