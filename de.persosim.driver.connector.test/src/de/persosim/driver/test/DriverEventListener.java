package de.persosim.driver.test;

public interface DriverEventListener {
	public abstract void messageReceivedCallback(String message);
	public abstract void handshakeMessageReceivedCallback(String event);
}
