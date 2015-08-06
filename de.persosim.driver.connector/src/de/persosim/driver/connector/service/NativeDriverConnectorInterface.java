package de.persosim.driver.connector.service;

import java.io.IOException;
import java.net.UnknownHostException;

import de.persosim.driver.connector.VirtualReaderUi;

public interface NativeDriverConnectorInterface {
	
	/**
	 * This method connects to the native driver part.
	 * 
	 * @throws IOException
	 * @throws UnknownHostException
	 */
	void connect(String nativeDriverHostName, int nativeDriverPort) throws IOException;

	boolean isRunning();

	/**
	 * This method disconnects from the native driver part.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	void disconnect() throws IOException, InterruptedException;

	/**
	 * Add an additional user interface.
	 * 
	 * @param ui
	 */
	void addUi(VirtualReaderUi ui);

	/**
	 * Remove a user interface.
	 * 
	 * @param ui
	 */
	void removeUi(VirtualReaderUi ui);
	
}
