package ch.fhnw.bacnetit.ase.encoding.api;

import java.net.URI;

import ch.fhnw.bacnetit.ase.encoding.TransportError;

public class T_ReportIndication {

	private final URI destinationAddress;
	private final Object context;
	private final TransportError transportError;

	public T_ReportIndication(final URI destinationAddress, final Object context, final TransportError transportError) {
		this.destinationAddress = destinationAddress;
		this.context = context;
		this.transportError = transportError;
	}

	public URI getDestinationAddress() {
		return this.destinationAddress;
	}

	public Object getContext() {
		return this.context;
	}

	public TransportError getTransportError() {
		return this.transportError;
	}
}
