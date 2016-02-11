package de.persosim.driver.connector;

import java.io.IOException;

import de.persosim.driver.connector.service.NativeDriverConnectorImpl;
import de.persosim.driver.connector.service.NativeDriverConnector;

public class DriverConnectorFactoryImpl implements DriverConnectorFactory {

	private NativeDriverConnector connector;
	private String contextIdentifier;

	@Override
	public NativeDriverConnector getConnector(String contextIdentifier) throws IOException {
		if (connector == null) {
			connector = new NativeDriverConnectorImpl();
			this.contextIdentifier = contextIdentifier;
		}
		if (this.contextIdentifier.equals(contextIdentifier)){
			return connector;	
		}
		throw new IOException("No connector can be created for the given context " + contextIdentifier);
	}

	@Override
	public void returnConnector(NativeDriverConnector connector) {
		if (this.connector == connector) {
			try {
				connector.disconnect();
			} catch (IOException | InterruptedException e) {
				// as this is only for cleanup in case the client did not call
				// disconnect exceptions are expected
			}
			this.connector = null;
			return;
		}
		throw new IllegalArgumentException("This connector was not given out by this factory");
	}

	@Override
	public boolean isConnectorAvailable() {
		return connector == null;
	}

}
