package de.persosim.driver.connector.pcsc;

import de.persosim.driver.connector.service.NativeDriverConnectorInterface;

public interface ConnectorEnabled {
	public abstract void setConnector(NativeDriverConnectorInterface connector);
}
