package uni.rostock.de.bacnet.it.coap.transportbinding;

import ch.fhnw.bacnetit.ase.encoding.api.TPDU;

public interface ResponseCallback {
	public void sendResponse(TPDU payload);
}
