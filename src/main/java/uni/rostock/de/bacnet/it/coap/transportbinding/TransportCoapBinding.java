package uni.rostock.de.bacnet.it.coap.transportbinding;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.fhnw.bacnetit.ase.application.service.api.TransportBindingService;
import ch.fhnw.bacnetit.ase.encoding.TransportError;
import ch.fhnw.bacnetit.ase.encoding.TransportError.TransportErrorType;
import ch.fhnw.bacnetit.ase.encoding._ByteQueue;
import ch.fhnw.bacnetit.ase.encoding.api.BACnetEID;
import ch.fhnw.bacnetit.ase.encoding.api.TPDU;
import ch.fhnw.bacnetit.ase.encoding.api.T_ReportIndication;
import ch.fhnw.bacnetit.ase.encoding.api.T_UnitDataRequest;
import ch.fhnw.bacnetit.ase.transportbinding.service.api.ASEService;

public class TransportCoapBinding implements ASEService {

	private static Logger LOG = LoggerFactory.getLogger(TransportCoapBinding.class);
	private TransportBindingService transportBindingService;

	private CoapServer server;
	private String resource = "/transport";

	public void init() {
		this.server.start();
	}

	public void destroyCoapServer() {
		this.server.stop();
	}

	@Override
	public void doCancel(BACnetEID destination, BACnetEID source) {

	}

	@Override
	public void doRequest(T_UnitDataRequest t_unitDataRequest) {
		Object context = t_unitDataRequest.getContext();
		sendRequest(t_unitDataRequest.getData(), t_unitDataRequest.getDestinationAddress().toString() + resource,
				context);
	}

	@Override
	public void setTransportBindingService(TransportBindingService transportBindingService) {
		this.transportBindingService = transportBindingService;
	}

	public void createCoapServer(int portNumber) {
		server = new CoapServer(portNumber);
		server.add(new CoapResource("transport") {
			@Override
			public void handlePOST(CoapExchange exchange) {
				byte[] tpduMessageBA = exchange.getRequestPayload();
				/**
				 * Callback function listening to ASE response to send the TPDU message for the
				 * request received from remote BACnet-IT device
				 */
				ResponseCallback responseCallback = new ResponseCallback() {

					@Override
					public void sendResponse(TPDU tpdu) {
						System.out.println("coap payload sent to client!!");
						byte[] payloadBytes = tpduToByteArray(tpdu);
						exchange.respond(ResponseCode._UNKNOWN_SUCCESS_CODE, payloadBytes);
						System.out.println("coap payload sent to client!!");
					}
				};
				TPDU tpdu = byteArrayToTPDU(tpduMessageBA);
				transportBindingService.onIndication(tpdu, null, responseCallback);
				if (!tpdu.isConfirmedRequest()) {
					exchange.respond(ResponseCode._UNKNOWN_SUCCESS_CODE);
				}
			}
		});
	}

	private void sendRequest(TPDU payload, String uri, Object context) {
		CoapClient client = new CoapClient(uri);
		client.useCONs();
		try {
			byte[] payloadBytes = tpduToByteArray(payload);
			client.post(new CoapHandler() {

				@Override
				public void onLoad(CoapResponse response) {
					TPDU tpdu = byteArrayToTPDU(response.getPayload());
					transportBindingService.onIndication(tpdu, null, null);
				}

				@Override
				public void onError() {
					try {
						transportBindingService.reportIndication("message not sent", payload.getSourceEID(),
								new T_ReportIndication(new URI(uri), context,
										new TransportError(TransportErrorType.Undefined, 0)));
					} catch (URISyntaxException e) {
						e.printStackTrace();
					}
				}
			}, payloadBytes, 0);
		} catch (Exception e) {
			try {
				transportBindingService.reportIndication(e.getMessage(), payload.getSourceEID(), new T_ReportIndication(
						new URI(uri), context, new TransportError(TransportErrorType.Undefined, 0)));
			} catch (URISyntaxException e1) {
				LOG.error(e1.getMessage());
			}
		}
	}

	@Override
	public void connect(URI uri) {

	}

	public TPDU byteArrayToTPDU(byte[] msg) {
		TPDU tpdu = null;
		try {
			_ByteQueue byteQueue = new _ByteQueue(msg);
			tpdu = new TPDU(byteQueue);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return tpdu;
	}

	public byte[] tpduToByteArray(TPDU tpdu) {
		byte[] payloadBA = null;
		_ByteQueue queue = new _ByteQueue();
		tpdu.write(queue);
		payloadBA = queue.popAll();
		return payloadBA;
	}
}
