package uni.rostock.de.bacnet.it.coap.oobAuth;

import java.net.URI;
import java.net.URISyntaxException;

import ch.fhnw.bacnetit.ase.application.service.api.ASEServices;
import ch.fhnw.bacnetit.ase.encoding.api.BACnetEID;
import ch.fhnw.bacnetit.ase.encoding.api.TPDU;
import ch.fhnw.bacnetit.ase.encoding.api.T_UnitDataRequest;
import ch.fhnw.bacnetit.samplesandtests.api.deviceobjects.BACnetObjectIdentifier;
import ch.fhnw.bacnetit.samplesandtests.api.deviceobjects.BACnetObjectType;
import ch.fhnw.bacnetit.samplesandtests.api.deviceobjects.BACnetPropertyIdentifier;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.asdu.ASDU;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.asdu.IncomingRequestParser;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.exception.BACnetException;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.constructed.ServicesSupported;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.primitive.OctetString;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.primitive.UnsignedInteger;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.util.ByteQueue;
import ch.fhnw.bacnetit.samplesandtests.api.service.confirmed.WritePropertyRequest;

public class ApplicationMessages {

	public static ASDU getServiceFromBody(byte[] body) {
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

	public static void sendWritePropertyRequest(ASEServices aseService, byte[] message, BACnetEID sourceEId,
			BACnetEID destinationEId, String uri) {
		WritePropertyRequest writePropertyRequest = new WritePropertyRequest(
				new BACnetObjectIdentifier(BACnetObjectType.analogValue, 1), BACnetPropertyIdentifier.presentValue,
				new UnsignedInteger(55), new OctetString(message), new UnsignedInteger(1));
		final ByteQueue byteQueue = new ByteQueue();
		writePropertyRequest.write(byteQueue);

		final TPDU tpdu = new TPDU(sourceEId, destinationEId, byteQueue.popAll());

		T_UnitDataRequest unitDataRequest = null;
		try {
			unitDataRequest = new T_UnitDataRequest(new URI(uri), tpdu, 1, null);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		aseService.doRequest(unitDataRequest);
	}

}
