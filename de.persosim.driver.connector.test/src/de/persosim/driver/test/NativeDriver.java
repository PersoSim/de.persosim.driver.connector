package de.persosim.driver.test;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

/**
 * This class can be used for testcases that need an open native driver server
 * socket for testing.
 * 
 * @author mboonk
 * 
 */
public class NativeDriver {
	public static final int PORT_NUMBER_DATA_DEFAULT = 5678;
	public static final int PORT_NUMBER_EVENTS_DEFAULT = 5679;
	private Collection<DriverEventListener> listeners = new HashSet<DriverEventListener>();

	private Thread eventThread;

	private DataThread dataThread;

	/**
	 * Creates a {@link NativeDriver} using the default 5678 port for data and
	 * 5679 for events.
	 * 
	 * @throws IOException
	 */
	public NativeDriver() throws IOException {
		this(PORT_NUMBER_DATA_DEFAULT, PORT_NUMBER_EVENTS_DEFAULT);
	}

	public NativeDriver(int portData, int portEvents) throws IOException {
		dataThread = new DataThread(portData, listeners);
		eventThread = new EventThread(portEvents, listeners);
		dataThread.setDaemon(true);
		eventThread.setDaemon(true);
		dataThread.start();
		eventThread.start();
	}

	public void stopDriver() throws InterruptedException {
		dataThread.interrupt();
		eventThread.interrupt();
		eventThread.join();
		dataThread.join();
	}

	public String sendData(String message) throws IOException, InterruptedException {
		return dataThread.sendData(message);
	}

	public void addListener(DriverEventListener listener) {
		listeners.add(listener);
	}

	public void removeListener(DriverEventListener listener) {
		listeners.remove(listener);
	}
}
