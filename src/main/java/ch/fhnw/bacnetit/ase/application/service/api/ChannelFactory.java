package ch.fhnw.bacnetit.ase.application.service.api;

import ch.fhnw.bacnetit.ase.application.service.ASEChannel;

/**
 * class ChannelFactoty. <br>
 * This class is used to create an instance of ASE channel.
 * 
 * @author vik
 *
 */
public class ChannelFactory {

	private ChannelFactory() {

	}

	/**
	 * This method returns an instance of {@link ASEChannel}.
	 * 
	 * @return
	 */
	public static ch.fhnw.bacnetit.ase.application.service.api.ASEServices getInstance() {
		try {
			
			return new ch.fhnw.bacnetit.ase.application.service.ASEChannel(new ChannelFactory());
			
		} catch (final Exception e) {
			System.err.println(e);
			return null;
		}
	}

}
