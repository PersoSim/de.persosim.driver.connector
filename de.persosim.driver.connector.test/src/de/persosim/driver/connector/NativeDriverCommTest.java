package de.persosim.driver.connector;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import mockit.Expectations;
import mockit.Mocked;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.connector.pcsc.PcscCallResult;
import de.persosim.driver.connector.pcsc.PcscConstants;
import de.persosim.driver.connector.pcsc.PcscListener;
import de.persosim.driver.connector.pcsc.SimplePcscCallResult;
import de.persosim.driver.test.ConnectorTest;
import de.persosim.driver.test.TestDriver;

public class NativeDriverCommTest extends ConnectorTest {

	private VirtualDriverComm nativeCommunication;
	@Mocked
	private PcscListener mockedListener;
	private TestDriver driver;
	private List<PcscListener> listeners;

	@Before
	public void setUp() throws Exception {
		driver = new TestDriver();
		driver.start(getTestDriverServerSocket());
		listeners = new ArrayList<>();
		nativeCommunication = new VirtualDriverComm(getTestDriverHost(), getTestDriverPort());
		nativeCommunication.setListeners(listeners);
		nativeCommunication.start();

		while (!nativeCommunication.isConnected()) {
			Thread.sleep(5);
		}
	}

	@After
	public void tearDown() throws Exception {
		nativeCommunication.stop();
		driver.stop();
	}

	/**
	 * Positive test using valid PCSC data.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testThreadValidPcsc() throws IOException, InterruptedException {
		// prepare the mock

		listeners.add(mockedListener);
		new Expectations() {
			{
				mockedListener.processPcscCall((PcscCallData) any);
				result = new SimplePcscCallResult(PcscConstants.IFD_SUCCESS);
			}
		};

		String data = driver.sendData(new UnsignedInteger(0), new UnsignedInteger(0), new byte[0]);
		assertEquals(PcscConstants.IFD_SUCCESS, UnsignedInteger
				.parseUnsignedInteger(data.split(Pattern.quote(NativeDriverInterface.MESSAGE_DIVIDER))[0], 16));
	}

	/**
	 * Negative test using invalid data. An IFD_NOT_SUPPORTED is expected and the
	 * listeners process method should not be called.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testThreadInvalidPcsc() throws IOException, InterruptedException {
		// prepare the mock

		listeners.add(mockedListener);
		new Expectations() {
			{
				mockedListener.processPcscCall((PcscCallData) any);
				times = 0;
				result = new SimplePcscCallResult(PcscConstants.IFD_SUCCESS);
			}
		};

		String data = driver.sendDataDirect(new UnsignedInteger(0), "j09uc540q93rufq09u,³48	q3m05ru");
		assertEquals(PcscConstants.IFD_NOT_SUPPORTED, UnsignedInteger
				.parseUnsignedInteger(data.split(Pattern.quote(NativeDriverInterface.MESSAGE_DIVIDER))[0], 16));
	}

	/**
	 * Checks if the listeners are processed in the correct order
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testListenerProcessingOrder() throws IOException, InterruptedException {
		List<PcscListener> listeners = new LinkedList<>();

		listeners.add(new PcscListener() {

			@Override
			public PcscCallResult processPcscCall(PcscCallData data) {
				assertEquals(0, data.getFunction().getAsSignedLong());
				data.setFunction(new UnsignedInteger(1));
				return null;
			}
		});

		listeners.add(new PcscListener() {

			@Override
			public PcscCallResult processPcscCall(PcscCallData data) {
				assertEquals(1, data.getFunction().getAsSignedLong());
				return null;
			}
		});

		nativeCommunication.setListeners(listeners);

		String result = driver.sendData(new UnsignedInteger(0), new UnsignedInteger(0));
		// this checks, if the native connector was the last listener to process because
		// both listeners added in this testcase do not return an answer
		assertEquals(PcscConstants.IFD_NOT_SUPPORTED.getAsHexString(), result);
	}

	/**
	 * Checks if an answer by a listener stops processing
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testListenerProcessingOrderEarlyAnswer() throws IOException, InterruptedException {
		List<PcscListener> listeners = new LinkedList<>();

		listeners.add(new PcscListener() {

			@Override
			public PcscCallResult processPcscCall(PcscCallData data) {
				return new SimplePcscCallResult(PcscConstants.IFD_SUCCESS);
			}
		});
		
		nativeCommunication.setListeners(listeners);

		String result = driver.sendData(new UnsignedInteger(0), new UnsignedInteger(0));
		assertEquals(PcscConstants.IFD_SUCCESS.getAsHexString(), result);
	}
}
