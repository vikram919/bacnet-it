/**
 *
 */
package ch.fhnw.bacnetit.ase.transportbinding.service.api;

import java.net.URI;

import ch.fhnw.bacnetit.ase.application.service.api.TransportBindingService;
import ch.fhnw.bacnetit.ase.encoding.api.BACnetEID;
import ch.fhnw.bacnetit.ase.encoding.api.T_UnitDataRequest;

public interface ASEService {
    public void doCancel(BACnetEID destination, BACnetEID source);

    public void doRequest(T_UnitDataRequest t_unitDataRequest);
    
    public void connect(URI uri);

    public void setTransportBindingService(
            TransportBindingService transportBindingService);

}
