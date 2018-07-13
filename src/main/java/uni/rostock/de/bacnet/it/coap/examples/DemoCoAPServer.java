package uni.rostock.de.bacnet.it.coap.examples;

import java.util.Arrays;
import java.util.List;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.ServerMessageDeliverer;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.scandium.util.ByteArrayUtils;

public class DemoCoAPServer {

	public static void main(String[] args) {
		CoapServer server = new CoapServer(5683);
		byte[] currentEtag = "dummy etag".getBytes();
		server.add(new CoapResource("light") {
			@Override
			public void handleGET(CoapExchange exchange) {
				if(exchange.getRequestOptions().hasIfNoneMatch()) {
					List<byte[]> receivedEtag = exchange.getRequestOptions().getETags();
					if(Arrays.equals(receivedEtag.get(0), currentEtag)) {
						exchange.respond(ResponseCode.PRECONDITION_FAILED);
					} else {
						/** server will process the request as if the condtional request is not present*/
						exchange.setETag(currentEtag);
						byte[] payload = "what ever your resource represntation is".getBytes();
						exchange.respond(ResponseCode._UNKNOWN_SUCCESS_CODE, payload);
					}
				}
			}
		});
		server.start();
	}
}
