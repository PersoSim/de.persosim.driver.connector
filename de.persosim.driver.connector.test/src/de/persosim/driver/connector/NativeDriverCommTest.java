package de.persosim.driver.connector;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Pattern;

import mockit.Expectations;
import mockit.Mocked;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.connector.pcsc.PcscConstants;
import de.persosim.driver.connector.pcsc.PcscListener;
import de.persosim.driver.test.TestDriver;

public class NativeDriverCommTest {
	
	private NativeDriverComm nativeComm;
	@Mocked
	private PcscListener mockedListener;
	private TestDriver driver;
	private Collection<PcscListener> listeners;

	@Before
	public void setUp() throws Exception{
		driver = new TestDriver();
		driver.start();
		listeners = new HashSet<>();
		nativeComm = new NativeDriverComm("localhost", TestDriver.PORT_NUMBER_DEFAULT, listeners);
		nativeComm.setName("NativeDriverCommunicationThread");	}
	
	@After
	public void tearDown() throws Exception {
		nativeComm.interrupt();
		driver.stop();
	}

	@Test
	public void testThreadValidPcsc() throws IOException, InterruptedException{
//		prepare the mock

		listeners.add(mockedListener);
		new Expectations() {{
			mockedListener.processPcscCall((PcscCallData) any);
			result = null;
		}};
		
		nativeComm.start();
		
		// XXX find better solution for timing issues during tests
		Thread.sleep(100);
		
		String data = driver.sendData((byte)0, (byte)0, new byte [0]);
		assertEquals(PcscConstants.IFD_NOT_SUPPORTED, Integer.parseInt(data.split(Pattern.quote(NativeDriverInterface.MESSAGE_DIVIDER))[0], 16));
	}
}
