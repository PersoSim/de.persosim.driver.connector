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
import de.persosim.driver.connector.pcsc.SimplePcscCallResult;
import de.persosim.driver.test.ConnectorTest;
import de.persosim.driver.test.TestDriver;

public class NativeDriverCommTest extends ConnectorTest{
	
	private NativeDriverComm nativeComm;
	private Thread nativeCommThread;
	@Mocked
	private PcscListener mockedListener;
	private TestDriver driver;
	private Collection<PcscListener> listeners;

	@Before
	public void setUp() throws Exception{
		driver = new TestDriver();
		driver.start(TESTDRIVER_PORT);
		listeners = new HashSet<>();
		nativeComm = new NativeDriverComm(TESTDRIVER_HOST, TESTDRIVER_PORT, listeners);
		nativeCommThread = new Thread(nativeComm);
		}
	
	@After
	public void tearDown() throws Exception {
		nativeComm.disconnect();
		nativeCommThread.join();
		driver.stop();
	}

	/**
	 * Positive test using valid PCSC data.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testThreadValidPcsc() throws IOException, InterruptedException{
//		prepare the mock

		listeners.add(mockedListener);
		new Expectations() {{
			mockedListener.processPcscCall((PcscCallData) any);
			result = new SimplePcscCallResult(PcscConstants.IFD_SUCCESS);
		}};
		
		nativeCommThread.start();
		
		while (!nativeComm.isConnected()){
			Thread.sleep(5);
		}
		
		String data = driver.sendData(new UnsignedInteger(0), new UnsignedInteger(0), new byte [0]);
		assertEquals(PcscConstants.IFD_SUCCESS, UnsignedInteger.parseUnsignedInteger(data.split(Pattern.quote(NativeDriverInterface.MESSAGE_DIVIDER))[0], 16));
	}

	/**
	 * Negative test using invalid data. An IFD_NOT_SUPPORTED is expected and
	 * the listeners process method should not be called.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testThreadInvalidPcsc() throws IOException, InterruptedException{
//		prepare the mock

		listeners.add(mockedListener);
		new Expectations() {{
			mockedListener.processPcscCall((PcscCallData) any);
			times = 0;
			result = new SimplePcscCallResult(PcscConstants.IFD_SUCCESS);
		}};
		
		nativeCommThread.start();
		
		while (!nativeComm.isConnected()){
			Thread.sleep(5);
		}
		
		String data = driver.sendDataDirect(new UnsignedInteger(0), "j09uc540q93rufq09u,³48	q3m05ru");
		assertEquals(PcscConstants.IFD_NOT_SUPPORTED, UnsignedInteger.parseUnsignedInteger(data.split(Pattern.quote(NativeDriverInterface.MESSAGE_DIVIDER))[0], 16));
	}
}
