package de.persosim.driver.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.Before;

/**
 * Parent class for all tests concerning the driver connector. It provides
 * functionality for host and port configuration of the used components.
 * 
 * @author mboonk
 *
 */
public class ConnectorTest {
	private ServerSocket simulatorServerSocket;
	private ServerSocket testDriverServerSocket;

	@Before
	public void reset() throws IOException {
		if (simulatorServerSocket != null) {
			simulatorServerSocket.close();
		}
		if (testDriverServerSocket != null) {
			testDriverServerSocket.close();
		}
		simulatorServerSocket = new ServerSocket(0);
		testDriverServerSocket = new ServerSocket(0);
	}

	/**
	 * @return The {@link ServerSocket} to be used for the test driver
	 * @throws IOException
	 */
	public ServerSocket getTestDriverServerSocket() throws IOException {
		return testDriverServerSocket;
	}

	/**
	 * @return A {@link Socket} that can be used for communication with the test
	 *         driver
	 * @throws IOException
	 */
	public Socket getTestDriverSocket() throws IOException {
		return new Socket("localhost", testDriverServerSocket.getLocalPort());
	}

	/**
	 * @return The {@link ServerSocket} to be used for the test simulator
	 * @throws IOException
	 */
	public ServerSocket getSimulatorServerSocket() throws IOException {
		return simulatorServerSocket;
	}

	/**
	 * @return A {@link Socket} that can be used for communication with the test
	 *         simulator
	 * @throws IOException
	 */
	public Socket getSimulatorSocket() throws IOException {
		return new Socket("localhost", simulatorServerSocket.getLocalPort());
	}
}
