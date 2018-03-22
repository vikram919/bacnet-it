package ch.fhnw.bacnetit.ase.application.service.api;

import java.net.URI;

import ch.fhnw.bacnetit.ase.encoding.api.BACnetEID;

/**
 * interface BACnetEntityListener. Interface BACnetEntityListener is implemented
 * by a member of the group which is configured to listen at multicast and
 * broadcast addresses.
 * 
 * @author vik
 *
 */
public interface BACnetEntityListener {

	public void onRemoteAdded(BACnetEID eid, URI uri);

	public void onRemoteRemove(BACnetEID eid);

	public void onLocalRequested(BACnetEID eid);
}
