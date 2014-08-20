package de.persosim.driver.test;

public interface DriverEventListener {
	public abstract void messageReceivedCallback(String message);
	public abstract void eventReceivedCallback(String event);
}
