package de.persosim.driver.connector;

import java.io.IOException;

import de.persosim.driver.connector.service.NativeDriverConnectorInterface;

/**
 * This creates {@link NativeDriverConnector} instances to interface with a
 * native driver.
 * 
 * @author mboonk
 *
 */
public interface DriverConnectorFactory {
	/**
	 * Provides a connector if available.
	 * 
	 * @return the connector object
	 * @param contextIdentifier
	 *            the context of the connector to be returned
	 * @throws IOException
	 *             when no connector object could be created
	 */
	public NativeDriverConnectorInterface getConnector(String contextIdentifier) throws IOException;

	/**
	 * Frees a previously created connector allocation.
	 * 
	 * @param connector
	 *            the {@link NativeDriverConnector} to be given back
	 * @throws IllegalArgumentException
	 *             when the connector to be given back was not created by this
	 *             factory
	 */
	public void returnConnector(NativeDriverConnectorInterface connector);

	/**
	 * Checks for connector availability.
	 * 
	 * @return true, if a fresh connector can be given out
	 */
	public boolean isConnectorAvailable();
}
