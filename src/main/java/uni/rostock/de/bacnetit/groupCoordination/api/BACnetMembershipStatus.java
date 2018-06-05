package uni.rostock.de.bacnetit.groupCoordination.api;

import ch.fhnw.bacnetit.ase.encoding._Enumerated;

/**
 * class BACnetMembershipStatus. <br>
 * As per BACnet/IT addendum135-2016bj-april-1, section ZZ.4.2 defines
 * BACnetMemebership status as an element of Group_Element structure.
 * BACnetMembershipStatus defines the status of membership registration.
 * 
 * @author Vikram
 *
 */
public class BACnetMembershipStatus extends _Enumerated {

	private static final long serialVersionUID = -226789920830116115L;

	public BACnetMembershipStatus(int value) {
		super(value);
	}

	/*
	 * unknown - status of membership is unknown and forwarding of requests to
	 * members is disabled
	 */
	public static final BACnetMembershipStatus unknown = new BACnetMembershipStatus(0);
	/* INVALID - BACnetEID and other attributes of member element are invalid */
	public static final BACnetMembershipStatus invalid = new BACnetMembershipStatus(1);
	/*
	 * configured - device is persistently configured in this property and Dynamic
	 * registrations are ignored
	 */
	public static final BACnetMembershipStatus configured = new BACnetMembershipStatus(2);
	/*
	 * registered - device is registered with remaining time as compared to
	 * TimeToLive element value
	 */
	public static final BACnetMembershipStatus registered = new BACnetMembershipStatus(3);
	/*
	 * learned - this status indicate that device was learned from discovery (not
	 * used so far may be used with BACnet/NL device configured by BACnet/NL proxy).
	 */
	public static final BACnetMembershipStatus learned = new BACnetMembershipStatus(4);
	/*
	 * expired - this status indicate that device is registered as member but its
	 * TimeToLive element value is completed, the device group object can remove the
	 * members with this status and provide space.
	 */
	public static final BACnetMembershipStatus expired = new BACnetMembershipStatus(5);
}
