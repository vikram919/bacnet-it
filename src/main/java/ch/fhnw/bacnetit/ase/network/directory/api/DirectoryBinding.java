package ch.fhnw.bacnetit.ase.network.directory.api;

import java.net.URI;
import java.util.List;

import ch.fhnw.bacnetit.ase.encoding.api.BACnetEID;

/**
 * interface DirectoryBinding. <br>
 * This interface DirectoryBinding implements the BACnet directory
 * functionality.
 * 
 * @author vik
 *
 */
public interface DirectoryBinding {
	public List<BACnetEID> findBDS();

	/**
	 * This method resolves {@link BACnetEID} to the respective URL, for both
	 * devices and device Groups.
	 * 
	 * @param eid
	 * @return
	 */
	public URI resolve(BACnetEID eid);

	public void delete(BACnetEID eid);

	public void register(BACnetEID eid, URI url, boolean isBDS);

	public void registerObject(String instance, boolean isInstanceObjectName, String txtvers, BACnetEID bacnetEid,
			String oid_oname, int ttl, int quality);
}
