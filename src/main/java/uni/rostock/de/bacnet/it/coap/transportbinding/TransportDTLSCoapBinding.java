package uni.rostock.de.bacnet.it.coap.transportbinding;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ResponseCache;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.pskstore.InMemoryPskStore;
import org.eclipse.californium.scandium.dtls.pskstore.StaticPskStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.fhnw.bacnetit.ase.application.service.api.TransportBindingService;
import ch.fhnw.bacnetit.ase.encoding.TransportError;
import ch.fhnw.bacnetit.ase.encoding._ByteQueue;
import ch.fhnw.bacnetit.ase.encoding.TransportError.TransportErrorType;
import ch.fhnw.bacnetit.ase.encoding.api.BACnetEID;
import ch.fhnw.bacnetit.ase.encoding.api.TPDU;
import ch.fhnw.bacnetit.ase.encoding.api.T_ReportIndication;
import ch.fhnw.bacnetit.ase.encoding.api.T_UnitDataRequest;
import ch.fhnw.bacnetit.ase.transportbinding.service.api.ASEService;

public class TransportDTLSCoapBinding implements ASEService {

	private static Logger LOG = LoggerFactory.getLogger(TransportDTLSCoapBinding.class);
	private static String resource = "/transport";
	private static final String SERVER_NAME = "server";
	private static final String CLIENT_NAME = "client";
	private boolean pskMode = false;
	private boolean certificateMode = false;
	private boolean rpkMode = false;
	// loads all the certificates
	private static final String TRUST_NAME = null;
	private final String keyStoreLocation;
	private final String trustStoreLocation;
	private static final char[] TRUST_STORE_PASSWORD = "rootPass".toCharArray();
	private static final char[] KEY_STORE_PASSWORD = "endPass".toCharArray();
	private InMemoryPskStore serverPskStore = new InMemoryPskStore();
	private InMemoryPskStore clientPskStore = new InMemoryPskStore();
	private DTLSConnector clientDtlsConnector;

	private TransportBindingService transportBindingService;

	public TransportDTLSCoapBinding() {
		if (System.getProperty("java.runtime.name").equals("Android Runtime")) {
			LOG.debug("android runtime environment detected");
			this.keyStoreLocation = "certs/keyStore.p12";
			this.trustStoreLocation = "certs/trustStore.p12";
		} else {
			this.keyStoreLocation = "certs/keyStore.jks";
			this.trustStoreLocation = "certs/trustStore.jks";
		}
	}

	private CoapServer server;

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

	public void createSecureCoapServer(int portNumber) {
		server = new CoapServer();
		server.add(new CoapResource("transport") {
			@Override
			public void handlePOST(CoapExchange exchange) {
				byte[] msg = exchange.getRequestPayload();
				ResponseCallback responseCallback = new ResponseCallback() {

					@Override
					public void sendResponse(TPDU tpdu) {
						byte[] payloadBytes = tpduToByteArray(tpdu);
						exchange.respond(ResponseCode._UNKNOWN_SUCCESS_CODE, payloadBytes);
					}
				};
				TPDU tpdu = byteArrayToTPDU(msg);
				transportBindingService.onIndication(tpdu, null, responseCallback);
				if (!tpdu.isConfirmedRequest()) {
					exchange.respond(ResponseCode._UNKNOWN_SUCCESS_CODE);
				}
			}
		});
		DtlsConnectorConfig.Builder config = new DtlsConnectorConfig.Builder();
		config.setAddress(new InetSocketAddress(portNumber));
		loadCredentials(config, SERVER_NAME);
		DTLSConnector connector = new DTLSConnector(config.build());
		CoapEndpoint.CoapEndpointBuilder builder = new CoapEndpoint.CoapEndpointBuilder();
		builder.setConnector(connector);
		server.addEndpoint(builder.build());
		server.start();
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

	public void createSecureCoapClient() {
		initEndpointManager();
	}

	private void initEndpointManager() {
		CoapEndpoint.CoapEndpointBuilder dtlsEndpointBuilder = new CoapEndpoint.CoapEndpointBuilder();
		DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder();
		dtlsConfig.setClientOnly();
		loadCredentials(dtlsConfig, CLIENT_NAME);
		clientDtlsConnector = new DTLSConnector(dtlsConfig.build());
		dtlsEndpointBuilder.setConnector(clientDtlsConnector);
		EndpointManager.getEndpointManager().setDefaultEndpoint(dtlsEndpointBuilder.build());
	}

	public void addPSK(String pskId, byte[] pskBytes, InetSocketAddress peerSocketAddress) {
		serverPskStore.setKey(pskId, pskBytes);
		clientPskStore.addKnownPeer(peerSocketAddress, pskId, pskBytes);
	}

	public void loadCredentials(DtlsConnectorConfig.Builder dtlsConfig, String alias) {

		try {

			SslContextUtil.Credentials endpointCredentials = SslContextUtil.loadCredentials(
					SslContextUtil.CLASSPATH_SCHEME + keyStoreLocation, alias, KEY_STORE_PASSWORD, KEY_STORE_PASSWORD);
			Certificate[] trustedCertificates = SslContextUtil.loadTrustedCertificates(
					SslContextUtil.CLASSPATH_SCHEME + trustStoreLocation, TRUST_NAME, TRUST_STORE_PASSWORD);

			if (pskMode) {
				// if (alias == SERVER_NAME) {
				// dtlsConfig.setPskStore(serverPskStore);
				// } else {
				// dtlsConfig.setPskStore(clientPskStore);
				// }
				dtlsConfig.setPskStore(new StaticPskStore("password", "sesame".getBytes()));
			}
			if (certificateMode) {
				dtlsConfig.setTrustStore(trustedCertificates);
				dtlsConfig.setIdentity(endpointCredentials.getPrivateKey(), endpointCredentials.getCertificateChain(),
						false);
			}
			if (rpkMode) {
				dtlsConfig.setIdentity(endpointCredentials.getPrivateKey(), endpointCredentials.getCertificateChain(),
						true);
			}
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setPskMode() {
		this.pskMode = true;
	}

	public void setRpkMode() {
		this.rpkMode = true;
	}

	public void setCertificateMode() {
		this.certificateMode = true;
	}

	public void setAllModes() {
		this.pskMode = true;
		this.rpkMode = true;
		this.certificateMode = true;
	}

	@Override
	public void connect(URI uri) {
		// this method not for CoAP

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
