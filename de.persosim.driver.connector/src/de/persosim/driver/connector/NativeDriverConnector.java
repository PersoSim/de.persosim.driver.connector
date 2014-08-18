package de.persosim.driver.connector;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;

import de.persosim.driver.connector.pcsc.PcscListener;

/**
 * This class handles the connection to the native part of the PersoSim driver
 * package via a socket Connection. In PCSC terms this would represent
 * functionality like an IFD Handler presents to the upper layers as described
 * in the PCSC specification part 3.
 * 
 * @author mboonk
 * 
 */
public class NativeDriverConnector {

	Collection<PcscListener> listeners = new HashSet<PcscListener>();
	private Socket nativeSocket;
	private NativeDriverComm comm;

	public NativeDriverConnector() {
	}
	
	/**
	 * This method connects to the native driver part.
	 * 
	 * @throws IOException
	 * @throws UnknownHostException
	 */
	public void connect(String hostName, int port) throws UnknownHostException,
			IOException {
		nativeSocket = new Socket(hostName, port);
		nativeSocket.getOutputStream().write("Card Inserted".getBytes());
		comm = new NativeDriverComm(nativeSocket, listeners);
		comm.start();
	}

	/**
	 * This method disconnects from the native driver part.
	 * 
	 * @throws IOException
	 */
	public void disconnect() throws IOException {
		comm.interrupt();
		nativeSocket.getOutputStream().write("Card Removed".getBytes());
		nativeSocket.close();
	}

	public void addListener(PcscListener listener) {
		listeners.add(listener);
	}

	public void removeListener(PcscListener listener) {
		listeners.remove(listener);
	}
}
