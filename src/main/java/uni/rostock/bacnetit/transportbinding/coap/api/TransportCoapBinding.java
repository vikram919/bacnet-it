package uni.rostock.bacnetit.transportbinding.coap.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.fhnw.bacnetit.ase.application.service.api.TransportBindingService;
import ch.fhnw.bacnetit.ase.encoding.api.BACnetEID;
import ch.fhnw.bacnetit.ase.encoding.api.TPDU;
import ch.fhnw.bacnetit.ase.encoding.api.T_UnitDataRequest;
import ch.fhnw.bacnetit.ase.transportbinding.service.api.ASEService;

public class TransportCoapBinding implements ASEService {

	private TransportBindingService transportBindingService;
	private static Logger LOG = LoggerFactory.getLogger(TransportCoapBinding.class);

	CoapClient client;
	CoapServer server;

	public void init() {
		server.start();
	}

	@Override
	public void doCancel(BACnetEID destination, BACnetEID source) {

	}

	@Override
	public void doRequest(T_UnitDataRequest t_unitDataRequest) {
		client.setURI(t_unitDataRequest.getDestinationAddress().toString());
		sendRequest(t_unitDataRequest.getData());
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
				byte[] msg = exchange.getRequestPayload();
				ByteArrayInputStream bis = new ByteArrayInputStream(msg);
				ObjectInput in = null;
				try {
					in = new ObjectInputStream(bis);
					TPDU tpdu = (TPDU) in.readObject();
					transportBindingService.onIndication(tpdu,
							new InetSocketAddress(exchange.getSourceAddress(), exchange.getSourcePort()));

				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
				} finally {
					try {
						if (in != null) {
							in.close();
						}
					} catch (IOException ex) {
					}
				}
			}
		});
	}

	public void sendRequest(TPDU payload) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		try {
			out = new ObjectOutputStream(bos);
			out.writeObject(payload);
			out.flush();
			byte[] payloadBytes = bos.toByteArray();
			client.post(new CoapHandler() {
				@Override
				public void onLoad(CoapResponse response) {
				}

				@Override
				public void onError() {
					System.err.println("FAILED");
				}
			}, payloadBytes, 0);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				bos.close();
			} catch (IOException ex) {
			}
		}
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
		}
	}

	public void createCoapClient() {
		client = new CoapClient();
	}

}
