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
	public static final String PROPERTY_NAME_DRIVER_PORT = "hjp.test.driver.port";
	public static final String PROPERTY_NAME_SIMULATOR_PORT = "hjp.test.simulator.port";
	private static final int SIMULATOR_PORT = 0;
	private static final int TESTDRIVER_PORT = 0;
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
		simulatorServerSocket = new ServerSocket(getSimulatorDriverPort());
		testDriverServerSocket = new ServerSocket(getTestDriverPort());
	}

	/**
	 * It uses the Java system property name {@value #PROPERTY_NAME_DRIVER_PORT}
	 * to be able to receive a value for the port to be used from the outside.
	 * Otherwise it returns the default value
	 * {@value ConnectorTest#TESTDRIVER_PORT}
	 * 
	 * @return
	 */
	private int getTestDriverPort() {
		return Integer.parseInt(System.getProperty(PROPERTY_NAME_DRIVER_PORT, TESTDRIVER_PORT + ""));
	}

	/**
	 * It uses the Java system property name
	 * {@value #PROPERTY_NAME_SIMULATOR_PORT} to be able to receive a value for
	 * the port to be used from the outside. Otherwise it returns the default
	 * value {@value #SIMULATOR_PORT}
	 * 
	 * @return
	 */
	private int getSimulatorDriverPort() {
		return Integer.parseInt(System.getProperty(PROPERTY_NAME_SIMULATOR_PORT, SIMULATOR_PORT + ""));
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
