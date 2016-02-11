package de.persosim.driver.connector.pcsc;

import de.persosim.driver.connector.service.NativeDriverConnector;

public interface ConnectorEnabled {
	public abstract void setConnector(NativeDriverConnector connector);
}
