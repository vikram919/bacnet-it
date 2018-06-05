package uni.rostock.de.bacnetit.groupCoordination.api;

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.fhnw.bacnetit.ase.application.service.api.ChannelConfiguration;
import ch.fhnw.bacnetit.ase.application.service.api.ChannelFactory;
import ch.fhnw.bacnetit.ase.application.transaction.api.ChannelListener;
import ch.fhnw.bacnetit.ase.encoding.api.BACnetEID;
import ch.fhnw.bacnetit.ase.encoding.api.TPDU;
import ch.fhnw.bacnetit.ase.encoding.api.T_ReportIndication;
import ch.fhnw.bacnetit.ase.encoding.api.T_UnitDataIndication;
import ch.fhnw.bacnetit.ase.encoding.api.T_UnitDataRequest;
import ch.fhnw.bacnetit.ase.transportbinding.service.api.ASEService;
import ch.fhnw.bacnetit.samplesandtests.api.deviceobjects.BACnetObjectIdentifier;
import ch.fhnw.bacnetit.samplesandtests.api.deviceobjects.BACnetObjectType;
import ch.fhnw.bacnetit.samplesandtests.api.deviceobjects.BACnetPropertyIdentifier;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.asdu.ASDU;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.asdu.ConfirmedRequest;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.asdu.IncomingRequestParser;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.asdu.UnconfirmedRequest;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.exception.BACnetException;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.constructed.SequenceOf;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.constructed.ServicesSupported;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.primitive.CharacterString;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.util.ByteQueue;
import ch.fhnw.bacnetit.samplesandtests.api.service.confirmed.AddListElementRequest;
import ch.fhnw.bacnetit.transportbinding.api.BindingConfiguration;
import ch.fhnw.bacnetit.transportbinding.api.ConnectionFactory;
import ch.fhnw.bacnetit.transportbinding.api.TransportBindingInitializer;
import ch.fhnw.bacnetit.transportbinding.ws.incoming.api.WSConnectionServerFactory;
import ch.fhnw.bacnetit.transportbinding.ws.outgoing.api.WSConnectionClientFactory;
import uni.rostock.de.bacnet.it.coap.crypto.EcdhHelper;
import uni.rostock.de.bacnet.it.coap.transportbinding.TransportDTLSCoapBinding;

public class DeviceMemberExample {

	private static final Logger LOG = LoggerFactory.getLogger(DeviceMemberExample.class);
	private ASEService aseService;
	private static final int wsServerPort = 8080;
	private static final int DTLS_SOCKET = 5684;
	private TransportDTLSCoapBinding coapDtlsbindingConfig;
	private static final int DEVICE_ID = 120;
	EcdhHelper ecdhHelper;

	public static void main(String[] args) {
		DeviceMemberExample deviceMember = new DeviceMemberExample();
		deviceMember.start();

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

		/* Adding a Device Group Object to the channel listener */
		channelConfigure.registerChannelListener(new ChannelListener(new BACnetEID(DEVICE_ID)) {

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

	public byte[] performRegisterOverBds(URI location) {
		final SequenceOf<CharacterString> uriChars = new SequenceOf<CharacterString>();
		uriChars.add(new CharacterString(location.toString()));
		/*
		 * when it is initial AddListElementRequest include listofGroupMembers property
		 * else it will be memberOf property
		 */
		final AddListElementRequest request = new AddListElementRequest(
				new BACnetObjectIdentifier(BACnetObjectType.group, 11), BACnetPropertyIdentifier.listOfGroupMembers,
				null, uriChars);
		final ByteQueue byteQueue = new ByteQueue();
		request.write(byteQueue);
		return byteQueue.popAll();
	}

	public void sendBacnetRegisterMessage() {
		try {
			ByteQueue queue = new ByteQueue();
			queue = new ByteQueue(performRegisterOverBds(new URI("coap://127.0.0.1:5684")));
			final TPDU tpdu = new TPDU(new BACnetEID(DEVICE_ID), new BACnetEID(445), queue.popAll());
			final T_UnitDataRequest unitDataRequest = new T_UnitDataRequest(new URI("coap://127.0.0.1:5684"), tpdu, 1,
					null);
			aseService.doRequest(unitDataRequest);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

}
