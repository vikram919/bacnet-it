package uni.rostock.de.bacnet.it.coap.examples;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.californium.core.coap.Request;

import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.constructed.DeviceObjectPropertyValue;
import uni.rostock.de.bacnet.it.coap.messageType.DeviceKeyExchange;

public class DemoCoAPClient {

	public static void main(String[] args) {
		
		CoapClient client = new CoapClient("coap://localhost:5683/light");
//		client.putIfNoneMatch("hello", MediaTypeRegistry.TEXT_PLAIN);
		client.post(new CoapHandler() {
			
			@Override
			public void onLoad(CoapResponse response) {
				System.out.println(response.getResponseText());
				
			}
			
			@Override
			public void onError() {
				// TODO Auto-generated method stub
				
			}
		}, "Hello".getBytes(), 0);
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		client.get();

	}

}
