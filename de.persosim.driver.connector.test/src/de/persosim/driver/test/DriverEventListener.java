package de.persosim.driver.test;

/**
 * Classes that need to react to driver events can implement this class. This
 * interface exists for testing purposes.
 * 
 * @author mboonk
 *
 */
public interface DriverEventListener {
	public abstract void messageReceivedCallback(String message);

	public abstract void handshakeMessageReceivedCallback(String event);
}
