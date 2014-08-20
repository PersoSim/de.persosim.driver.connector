package de.persosim.driver.connector;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.persosim.driver.test.DriverEventListener;
import de.persosim.driver.test.NativeDriver;

public class NativeDriverConnectorTest extends TestCase implements DriverEventListener {
	private NativeDriverConnector connector;
	private NativeDriver driver;
	private String messageReceived;
	private String eventReceived;
	private int eventsReceived;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		driver = new NativeDriver();
		driver.addListener(this);
		connector = new NativeDriverConnector("localhost", NativeDriver.PORT_NUMBER_DATA_DEFAULT, NativeDriver.PORT_NUMBER_EVENTS_DEFAULT);
		messageReceived = null;
		eventReceived = null;
	}
	
	@Override
	@After
	protected void tearDown() throws Exception {
		driver.stopDriver();
	}


	@Override
	public void messageReceivedCallback(String message) {
		messageReceived = message;
	}

	@Override
	public void eventReceivedCallback(String event) {
		eventReceived = event;
		eventsReceived++;
	}
	
	@Test(timeout = 2000)
	public void testConnect() throws Exception {
		// call mut
		connector.connect();

		
		while(eventReceived == null){
			Thread.yield();
		}

		assertEquals(1, eventsReceived);		
		assertEquals("Card Inserted", eventReceived);
	}
	
	@Test(timeout=2000)
	public void testDisconnect() throws Exception{
		// call mut
		connector.connect();
		
		connector.disconnect();
		
		while(eventReceived == null){
			Thread.yield();
		}
		
		assertEquals(2, eventsReceived);
		assertEquals("Card Removed", eventReceived);
	}
	
}
