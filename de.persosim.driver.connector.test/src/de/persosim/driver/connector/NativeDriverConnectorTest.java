package de.persosim.driver.connector;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.persosim.driver.test.TestDriver;
import de.persosim.simulator.utils.HexString;

public class NativeDriverConnectorTest {
	
	private NativeDriverConnector nativeConnector;
	private TestDriver driver;

	@Before
	public void setUp() throws Exception{
		driver = new TestDriver();
		driver.start();
		nativeConnector = new NativeDriverConnector("localhost", TestDriver.PORT_NUMBER_DEFAULT);
		nativeConnector.connect();
	}
	
	@After
	public void tearDown() throws Exception {
		nativeConnector.disconnect();
		driver.stop();
	}

	@Test
	public void testPcscGetCapabilitiesVendorName() throws IOException, InterruptedException{
		System.out.println("entering test");
		//FIXME find better solution for timing issues while testing
		Thread.sleep(100);
		String tag = "00000100";
		
		String result = driver.sendData(0, NativeDriverComm.PCSC_FUNCTION_GET_CAPABILITIES, HexString.toByteArray(tag));
		
		String expected = PcscConstants.IFD_SUCCESS + "#" + tag;
		
		assertTrue(result.indexOf(expected) == 0);
	}

	@Test
	public void testPcscGetCapabilitiesNotExisting() throws IOException, InterruptedException{
		System.out.println("entering test");
		//FIXME find better solution for timing issues while testing
		Thread.sleep(100);
		String tag = "FFFFFFFF";
		
		String result = driver.sendData(0, NativeDriverComm.PCSC_FUNCTION_GET_CAPABILITIES, HexString.toByteArray(tag));
		
		String expected = PcscConstants.IFD_SUCCESS + "#" + tag;
		
		assertTrue(!(result.indexOf(expected) == 0));
	}
}
