/**
 *
 */
package ch.fhnw.bacnetit.ase.application.service.api;

import java.net.URI;

import ch.fhnw.bacnetit.ase.encoding.api.BACnetEID;
import ch.fhnw.bacnetit.ase.encoding.api.T_UnitDataRequest;

/**
 * @author IMVS, FHNW
 *
 */
public interface ApplicationService {

	public void doRequest(T_UnitDataRequest t_unitDataRequest);

	public void doCancel(BACnetEID destination, BACnetEID source);

	public void connect(URI uri);

}
