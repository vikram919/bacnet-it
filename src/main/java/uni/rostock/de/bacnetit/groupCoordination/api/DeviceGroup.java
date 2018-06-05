package uni.rostock.de.bacnetit.groupCoordination.api;

import java.util.LinkedList;
import java.util.List;

import ch.fhnw.bacnetit.ase.encoding.EntityRecord;
import ch.fhnw.bacnetit.ase.encoding.api.BACnetEID;
import ch.fhnw.bacnetit.ase.encoding.api.TPDU;
import ch.fhnw.bacnetit.ase.transportbinding.service.api.ASEService;
import ch.fhnw.bacnetit.samplesandtests.api.deviceobjects.BACnetPropertyIdentifier;

/**
 * class DeviceGroup. <br>
 * This class DeviceGroup supports DeviceGroupCoordination functionality for
 * device group management and communication.
 * 
 * @author vikram
 *
 */
public class DeviceGroup {

	private BACnetEID deviceGroupObject;
	/* This represents the device group member list with Member records */
	private List<EntityRecord> deviceGroupMembersList = new LinkedList<>();

	public DeviceGroup(BACnetEID deviceGroupObject) {
		this.deviceGroupObject = deviceGroupObject;
	}

	public BACnetEID getGroupIdentifier() {
		return this.deviceGroupObject;
	}

	public void addMember(EntityRecord memberRecord, BACnetPropertyIdentifier baCnetPropertyIdentifier) {
		// TODO: check the bacnetPropertyIdentifier for adding a new device to
		// deviceGroupMemberList or configuring the existing memeber of
		// deviceGroupMemberList
		if (baCnetPropertyIdentifier.equals(BACnetPropertyIdentifier.listOfGroupMembers)) {
			deviceGroupMembersList.add(memberRecord);
		}
		if (baCnetPropertyIdentifier.equals(BACnetPropertyIdentifier.memberOf)) {
			// TODO: update the group member record
		}

	}

	/**
	 * method deviceGroupCordinationFunction.<br>
	 * As per the BACnet/IT draft addendum135-2016-april-1-draft-2, ZZ.4.1 the
	 * DeviceGroupCoordination function receives the unconfirmed request on behalf
	 * of the device group and forwards these requests to all the members of the
	 * device group excluding the device which actually sent the request.
	 * 
	 * @param tpdu
	 *            - TPDU with source device BACnetEID and destination BACnetEID as
	 *            group identifier
	 */
	public void deviceGroupCoordinationFunction(TPDU tpdu, ASEService aseService) {
		// TODO: for loop and send the UnConfirmedRequest to all the devices in
		// deviceGroupMembersList
	}
}
