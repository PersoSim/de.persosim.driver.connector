package de.persosim.driver.test;

/**
 * Parent class for all tests concerning the driver connector. It provides
 * functionality for host and port configuration of the used components.
 * 
 * @author mboonk
 *
 */
public class ConnectorTest {
	public static final String PROPERTY_NAME_DRIVER_PORT = "hjp.test.driver.port";
	public static final String PROPERTY_NAME_DRIVER_HOST = "hjp.test.driver.host";
	public static final String PROPERTY_NAME_SIMULATOR_PORT = "hjp.test.simulator.port";
	public static final String PROPERTY_NAME_SIMULATOR_HOST = "hjp.test.simulator.host";
	private static final int SIMULATOR_PORT = 12345;
	private static final int TESTDRIVER_PORT = 12346;
	private static final String SIMULATOR_HOST = "localhost";
	private static final String TESTDRIVER_HOST = "localhost";

	/**
	 * It uses the Java system property name {@value #PROPERTY_NAME_DRIVER_PORT} to be
	 * able to receive a value for the port to be used from the outside.
	 * Otherwise it returns the default value
	 * {@value ConnectorTest#TESTDRIVER_PORT}
	 * 
	 * @return
	 */
	public int getTestDriverPort() {
		return Integer.parseInt(System.getProperty(PROPERTY_NAME_DRIVER_PORT, TESTDRIVER_PORT + ""));
	}

	/**
	 * It uses the Java system property name {@value #PROPERTY_NAME_DRIVER_HOST} to be
	 * able to receive a value for the port to be used from the outside.
	 * Otherwise it returns the default value {@value #TESTDRIVER_HOST}
	 * 
	 * @return
	 */
	public String getTestDriverHost() {
		return System.getProperty(PROPERTY_NAME_DRIVER_HOST, TESTDRIVER_HOST);
	}

	/**
	 * It uses the Java system property name {@value #PROPERTY_NAME_SIMULATOR_PORT} to
	 * be able to receive a value for the port to be used from the outside.
	 * Otherwise it returns the default value {@value #SIMULATOR_PORT}
	 * 
	 * @return
	 */
	public int getSimulatorDriverPort() {
		return Integer.parseInt(System.getProperty(PROPERTY_NAME_SIMULATOR_PORT, SIMULATOR_PORT + ""));
	}

	/**
	 * It uses the Java system property name {@value #PROPERTY_NAME_SIMULATOR_HOST} to
	 * be able to receive a value for the port to be used from the outside.
	 * Otherwise it returns the default value {@value #SIMULATOR_HOST}
	 * 
	 * @return
	 */
	public String getSimulatorDriverHost() {
		return System.getProperty(PROPERTY_NAME_SIMULATOR_HOST, SIMULATOR_HOST);
	}
}
