package uni.rostock.bacnetit.transportbinding.coap.examples;

// Make sure to import the following classes
// Import Java components
import java.net.URI;

import org.slf4j.LoggerFactory;

// Import packages from the BACnet/IT opensource projects
// By convention just classes within an api package should be used
import ch.fhnw.bacnetit.ase.application.configuration.api.DiscoveryConfig;
import ch.fhnw.bacnetit.ase.application.service.api.ASEServices;
import ch.fhnw.bacnetit.ase.application.service.api.BACnetEntityListener;
import ch.fhnw.bacnetit.ase.application.service.api.ChannelConfiguration;
import ch.fhnw.bacnetit.ase.application.service.api.ChannelFactory;
import ch.fhnw.bacnetit.ase.application.transaction.api.ChannelListener;
import ch.fhnw.bacnetit.ase.encoding.api.BACnetEID;
import ch.fhnw.bacnetit.ase.network.directory.api.DirectoryService;
import ch.fhnw.bacnetit.ase.transportbinding.service.api.ASEService;
import ch.fhnw.bacnetit.directorybinding.dnssd.api.DNSSD;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import uni.rostock.bacnetit.transportbinding.coap.api.TransportCoapBinding;

public class ConfiguratorCoAP {

	private static Logger LOG = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

	public static void main(String[] args) {
		LOG.setLevel(Level.OFF);

		/*
		 *********************** SETUP ASE 1 ***********************
		 */
		final ASEServices aseServicesChannel1 = ChannelFactory.getInstance();
		final ChannelConfiguration channelConfiguration1 = aseServicesChannel1;

		// Configure BACnetEntity Listener to handle Control Messages
		final BACnetEntityListener bacNetEntityHandler1 = new BACnetEntityListener() {

			@Override
			public void onRemoteAdded(final BACnetEID eid, final URI remoteUri) {
				DirectoryService.getInstance().register(eid, remoteUri, false, true);
			}

			@Override
			public void onRemoteRemove(final BACnetEID eid) {
			}

			@Override
			public void onLocalRequested(final BACnetEID eid) {
			}

		};

		channelConfiguration1.setEntityListener(bacNetEntityHandler1);
		int wsServerPort1 = 8080;
		TransportCoapBinding bindingConfiguration1 = new TransportCoapBinding();
		bindingConfiguration1.createCoapClient();
		bindingConfiguration1.createCoapServer(8080);
		bindingConfiguration1.init();
		channelConfiguration1.setASEService((ASEService) bindingConfiguration1);
		System.out.println("ASE channel1 is port binded to coap protocol:-)");

		/*
		 *********************** SETUP ASE 2 ***********************
		 */
		final ASEServices aseServicesChannel2 = ChannelFactory.getInstance();
		final ChannelConfiguration channelConfiguration2 = aseServicesChannel2;

		// Configure BACnetEntity Listener to handle Control Messages
		final BACnetEntityListener bacNetEntityHandler2 = new BACnetEntityListener() {

			@Override
			public void onRemoteAdded(final BACnetEID eid, final URI remoteUri) {
				DirectoryService.getInstance().register(eid, remoteUri, false, true);
			}

			@Override
			public void onRemoteRemove(final BACnetEID eid) {
			}

			@Override
			public void onLocalRequested(final BACnetEID eid) {
			}

		};

		channelConfiguration2.setEntityListener(bacNetEntityHandler2);

		int wsServerPort2 = 9090;
		
		TransportCoapBinding bindingConfiguration2 = new TransportCoapBinding();
		bindingConfiguration2.createCoapClient();
		bindingConfiguration2.createCoapServer(9090);
		bindingConfiguration2.init();
		channelConfiguration2.setASEService((ASEService) bindingConfiguration2);
		System.out.println("ASE channel2 is port binded to coap protocol:-)");

		/*
		 *********************** Register BACnet devices from application 1 in ASE 1 ***********************
		 */
		AbstractApplication application1 = new Application1(aseServicesChannel1);
		for (ChannelListener device : application1.devices) {
			channelConfiguration1.registerChannelListener(device);
		}

		/*
		 *********************** Register BACnet devices from application 2 in ASE 2 ***********************
		 */
		AbstractApplication application2 = new Application2(aseServicesChannel2);
		for (ChannelListener device : application2.devices) {
			channelConfiguration2.registerChannelListener(device);
		}

		/*
		 *********************** Initialize the directory service (not used in this example)
		 */
		final DiscoveryConfig ds = new DiscoveryConfig("DNSSD", "1.1.1.1", "itb.bacnet.ch.", "bds._sub._bacnet._tcp.",
				"dev._sub._bacnet._tcp.", "obj._sub._bacnet._tcp.", false);

		try {
			DirectoryService.init();
			DirectoryService.getInstance().setDNSBinding(new DNSSD(ds));

		} catch (final Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		/*
		 *********************** Enforce Application1 to send a ReadPropertyRequest to Application2.
		 * Application2 answers with its "value". To represent the ReadPropertyRequest
		 * and the ReadPropertyAck BACnet4J is used.***********************
		 */
		try {
			System.out.println("Applicatio1 sends a ReadPropRequest to Application2");
			application1.sendReadPropertyRequest(new URI("coap://localhost:" + wsServerPort2 + "/transport"),
					new BACnetEID(1001), new BACnetEID(2001));
		} catch (Exception e) {
			System.err.print(e);
		}

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		/*
		 *********************** Enforce Application2 to send a WhoIsRequest to Application1. Application1
		 * answers with an IAmRequest. To represent both BACnet services (WhoIsRequest
		 * and IAmRequest) a byte stream is provided. Therefore BACnet4J is not needed.
		 *
		 */

		// Represent a WhoIsRequest as byte array
		byte[] whoIsRequest = new byte[] { (byte) 0x1e, (byte) 0x8e, (byte) 0x8f, (byte) 0x1f };
		try {
			System.out.println("Applicatio2 sends a WhoIsRequest to Application1");
			application2.sendBACnetMessage(new URI("coap://localhost:" + wsServerPort1 + "/transport"),
					new BACnetEID(2001), new BACnetEID(1001), whoIsRequest);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Wait until close
		try {
			System.in.read();
		} catch (Exception e) {
		}

	}
}