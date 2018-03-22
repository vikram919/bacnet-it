package ch.fhnw.bacnetit.transportbinding.ws.examples;

//Make sure to import the following classes
//Import Java components
import java.net.URI;

import org.slf4j.LoggerFactory;

//Import packages from the BACnet/IT opensource projects
//By convention just classes within an api package should be used
import ch.fhnw.bacnetit.ase.application.configuration.api.DiscoveryConfig;
import ch.fhnw.bacnetit.ase.application.service.api.ASEServices;
import ch.fhnw.bacnetit.ase.application.service.api.BACnetEntityListener;
import ch.fhnw.bacnetit.ase.application.service.api.ChannelConfiguration;
import ch.fhnw.bacnetit.ase.application.service.api.ChannelFactory;
import ch.fhnw.bacnetit.ase.application.transaction.api.*;
import ch.fhnw.bacnetit.ase.encoding.api.BACnetEID;
import ch.fhnw.bacnetit.ase.network.directory.api.DirectoryService;
import ch.fhnw.bacnetit.ase.transportbinding.service.api.ASEService;
import ch.fhnw.bacnetit.directorybinding.dnssd.api.DNSSD;
import ch.fhnw.bacnetit.transportbinding.api.BindingConfiguration;
import ch.fhnw.bacnetit.transportbinding.api.ConnectionFactory;
import ch.fhnw.bacnetit.transportbinding.api.TransportBindingInitializer;
import ch.fhnw.bacnetit.transportbinding.ws.incoming.api.*;
import ch.fhnw.bacnetit.transportbinding.ws.outgoing.api.*;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class ConfiguratorWS {

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
				// TODO Auto-generated method stub
			}

			@Override
			public void onLocalRequested(final BACnetEID eid) {
				// TODO Auto-generated method stub
			}

		};

		channelConfiguration1.setEntityListener(bacNetEntityHandler1);

		// Configure the transport binding
		final ConnectionFactory connectionFactory1 = new ConnectionFactory();
		connectionFactory1.addConnectionClient("ws", new WSConnectionClientFactory());
		int wsServerPort1 = 8080;
		connectionFactory1.addConnectionServer("ws", new WSConnectionServerFactory(wsServerPort1));

		BindingConfiguration bindingConfiguration1 = new TransportBindingInitializer();
		bindingConfiguration1.initializeAndStart(connectionFactory1);
		channelConfiguration1.setASEService((ASEService) bindingConfiguration1);

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
				// TODO Auto-generated method stub
			}

			@Override
			public void onLocalRequested(final BACnetEID eid) {
				// TODO Auto-generated method stub
			}

		};

		channelConfiguration2.setEntityListener(bacNetEntityHandler2);

		// Configure the transport binding
		final ConnectionFactory connectionFactory2 = new ConnectionFactory();
		connectionFactory2.addConnectionClient("ws", new WSConnectionClientFactory());
		int wsServerPort2 = 9090;
		connectionFactory2.addConnectionServer("ws", new WSConnectionServerFactory(wsServerPort2));

		BindingConfiguration bindingConfiguration2 = new TransportBindingInitializer();
		bindingConfiguration2.initializeAndStart(connectionFactory2);
		channelConfiguration2.setASEService((ASEService) bindingConfiguration2);

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
			application1.sendReadPropertyRequest(new URI("ws://localhost:" + wsServerPort2), new BACnetEID(1001),
					new BACnetEID(2001));
		} catch (Exception e) {
			System.err.print(e);
		}

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
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
			application2.sendBACnetMessage(new URI("ws://localhost:" + wsServerPort1), new BACnetEID(2001),
					new BACnetEID(1001), whoIsRequest);
		} catch (Exception e) {
			System.out.println(e);
		}

		// Wait until close
		try {
			System.in.read();
		} catch (Exception e) {
		}

	}
}