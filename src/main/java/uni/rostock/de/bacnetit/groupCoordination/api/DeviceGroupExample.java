package uni.rostock.de.bacnetit.groupCoordination.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.fhnw.bacnetit.ase.application.service.api.ChannelConfiguration;
import ch.fhnw.bacnetit.ase.application.service.api.ChannelFactory;
import ch.fhnw.bacnetit.ase.application.transaction.api.ChannelListener;
import ch.fhnw.bacnetit.ase.encoding.api.BACnetEID;
import ch.fhnw.bacnetit.ase.encoding.api.BACnetEID.BACnetEIDOption;
import ch.fhnw.bacnetit.ase.encoding.api.T_ReportIndication;
import ch.fhnw.bacnetit.ase.encoding.api.T_UnitDataIndication;
import ch.fhnw.bacnetit.ase.transportbinding.service.api.ASEService;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.asdu.ASDU;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.asdu.ConfirmedRequest;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.asdu.IncomingRequestParser;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.asdu.UnconfirmedRequest;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.exception.BACnetException;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.constructed.ServicesSupported;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.util.ByteQueue;
import ch.fhnw.bacnetit.samplesandtests.api.service.confirmed.AddListElementRequest;
import ch.fhnw.bacnetit.transportbinding.api.BindingConfiguration;
import ch.fhnw.bacnetit.transportbinding.api.ConnectionFactory;
import ch.fhnw.bacnetit.transportbinding.api.TransportBindingInitializer;
import ch.fhnw.bacnetit.transportbinding.ws.incoming.api.WSConnectionServerFactory;
import ch.fhnw.bacnetit.transportbinding.ws.outgoing.api.WSConnectionClientFactory;
import uni.rostock.de.bacnet.it.coap.crypto.EcdhHelper;
import uni.rostock.de.bacnet.it.coap.transportbinding.TransportDTLSCoapBinding;

public class DeviceGroupExample {

	private static final Logger LOG = LoggerFactory.getLogger(DeviceGroupExample.class);
	private ASEService aseService;
	private static final int wsServerPort = 8080;
	private static final int DTLS_SOCKET = 5684;
	private TransportDTLSCoapBinding coapDtlsbindingConfig;
	private static final int GROUP1_ID = 445;
	EcdhHelper ecdhHelper;

	public static void main(String[] args) {

	}

	public void start() {

		aseService = (ASEService) ChannelFactory.getInstance();
		ChannelConfiguration channelConfigure = (ChannelConfiguration) aseService;

		/* Adding coap as transport binding to ASEService using channelConfigure */
		coapDtlsbindingConfig = new TransportDTLSCoapBinding();
		coapDtlsbindingConfig.setAllModes();
		coapDtlsbindingConfig.createSecureCoapClient();
		coapDtlsbindingConfig.createSecureCoapServer(DTLS_SOCKET);
		coapDtlsbindingConfig.init();
		channelConfigure.setASEService((ASEService) coapDtlsbindingConfig);

		/* Adding WebSockets as transport binding to ASEService */
		final ConnectionFactory connectionFactory1 = new ConnectionFactory();
		connectionFactory1.addConnectionClient("ws", new WSConnectionClientFactory());
		connectionFactory1.addConnectionServer("ws", new WSConnectionServerFactory(wsServerPort));
		BindingConfiguration wsBindingConfiguration = new TransportBindingInitializer();
		wsBindingConfiguration.initializeAndStart(connectionFactory1);
		channelConfigure.setASEService((ASEService) wsBindingConfiguration);

		/* Adding a Device Group Object to the channel listener */
		DeviceGroup deviceGroup1 = new DeviceGroup(new BACnetEID(GROUP1_ID, BACnetEIDOption.GROUP));
		channelConfigure.registerChannelListener(new ChannelListener(deviceGroup1.getGroupIdentifier()) {

			@Override
			public void onIndication(T_UnitDataIndication tUnitDataIndication, Object context) {
				LOG.debug("message T_unitDataIndication");
				ASDU receivedRequest = getServiceFromBody(tUnitDataIndication.getData().getBody());
				// TODO: DeviceGroupCoordinateFunction should act on the received request
				// either add the device to ListOfGroupMemebers or
				// send received unconfirmed request to all the members excluding the device
				// which sent the request
				// TODO: on receiving opt different transport binding and port to forward the
				// request to the members of the group
				
				// TODO: check the request is a AddListElement request or unconfirmed request
				if (((ConfirmedRequest) receivedRequest).getServiceRequest() instanceof AddListElementRequest) {
					// deviceGroup1.addMember(memberRecord, baCnetPropertyIdentifier);
				}
				if (receivedRequest instanceof UnconfirmedRequest) {
					// deviceGroup1.deviceGroupCoordinationFunction(tpdu);
				}

			}

			@Override
			public void onError(T_ReportIndication tReportIndication, String cause) {

			}
		});
	}

	public ASDU getServiceFromBody(byte[] body) {
		ASDU request = null;
		try {
			ByteQueue queue = new ByteQueue(body);
			ServicesSupported servicesSupported = new ServicesSupported();
			servicesSupported.setAll(true);
			IncomingRequestParser requestParser = new IncomingRequestParser(servicesSupported, queue);
			request = requestParser.parse();
		} catch (BACnetException e) {
			e.printStackTrace();
		}
		return request;
	}
}
