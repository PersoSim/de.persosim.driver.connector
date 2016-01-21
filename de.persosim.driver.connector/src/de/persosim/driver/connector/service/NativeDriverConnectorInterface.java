package de.persosim.driver.connector.service;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;

import de.persosim.driver.connector.VirtualReaderUi;
import de.persosim.driver.connector.pcsc.PcscListener;

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
	
	/**
	 * Add a listener that is to be informed about PCSC calls.
	 * 
	 * @param listener
	 */
	public void addListener(PcscListener listener);
	
	/**
	 * Remove a listener and stop informing it about PCSC calls.
	 * 
	 * @param listener
	 */
	public void removeListener(PcscListener listener);

	/**
	 * Returns a collection of references to the currently added {@link VirtualReaderUi} instances.
	 * 
	 * @return a collection of user interfaces currently attached to the connector
	 */
	public Collection<VirtualReaderUi> getUserInterfaces();

	/**
	 * Returns the currently active listeners as a list of actual references to the listeners.
	 * 
	 * @return the list of currently attached listeners
	 */
	public List<PcscListener> getListeners();
	
}
