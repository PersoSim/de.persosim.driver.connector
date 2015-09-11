package de.persosim.driver.connector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.globaltester.simulator.Simulator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.persosim.driver.connector.pcsc.AbstractPcscFeature;
import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.connector.pcsc.PcscCallResult;
import de.persosim.driver.connector.pcsc.PcscConstants;
import de.persosim.driver.connector.pcsc.PcscListener;
import de.persosim.driver.connector.pcsc.SimplePcscCallResult;
import de.persosim.driver.test.ConnectorTest;
import de.persosim.driver.test.TestDriver;
import de.persosim.simulator.platform.Iso7816;
import de.persosim.simulator.utils.HexString;
import de.persosim.simulator.utils.Utils;
import mockit.Mocked;
import mockit.NonStrictExpectations;

/**
 * This class tests the {@link NativeDriverConnector}.
 * @author mboonk
 *
 */
public class NativeDriverConnectorTest extends ConnectorTest{

	private NativeDriverConnector nativeConnector;
	private TestDriver driver;
	private final byte [] testAtr = "TESTATR".getBytes();
	@Mocked
	private Activator activator;
//	TestSocketSim sim;
//	@Mocked
//	private TestApduHandler handler;
	
	private Simulator simulator;
	
	//XXX use JUnit test rule with http://junit.org/apidocs/org/junit/rules/DisableOnDebug.html when using JUnit 4.12
	
	@Before
	public void setUp() throws Exception {
		
//		sim = new TestSocketSim(SIMULATOR_PORT);
//		sim.start();
		
		driver = new TestDriver();
		driver.start(TESTDRIVER_PORT);
				
		nativeConnector = new NativeDriverConnector();
		nativeConnector.connect(TESTDRIVER_HOST, TESTDRIVER_PORT);
		
		simulator = new Simulator() {
			
			@Override
			public boolean stopSimulator() {
				// TODO Auto-generated method stub
				return true;
			}
			
			@Override
			public boolean startSimulator() {
				// TODO Auto-generated method stub
				return true;
			}
			
			@Override
			public boolean restartSimulator() {
				// TODO Auto-generated method stub
				return true;
			}
			
			@Override
			public byte[] processCommand(byte[] apdu) {
				// TODO Auto-generated method stub
				if(Arrays.equals(apdu, HexString.toByteArray("FF01"))) {
					return testAtr;
				}
				
				if (Arrays.equals(apdu, HexString.toByteArray("FF00"))) {
					return HexString.toByteArray("9000");
				}
				
				if (Arrays.equals(apdu, HexString.toByteArray("00000000"))) {
					return "RESPONSE".getBytes();
				}
				
				return null;
			}
			
			@Override
			public boolean isRunning() {
				// TODO Auto-generated method stub
				return true;
			}
			
			@Override
			public byte[] cardReset() {
				// TODO Auto-generated method stub
				return testAtr;
			}
			
			@Override
			public byte[] cardPowerUp() {
				// TODO Auto-generated method stub
				return testAtr;
			}
			
			@Override
			public byte[] cardPowerDown() {
				// TODO Auto-generated method stub
				return Utils.toUnsignedByteArray(Iso7816.SW_9000_NO_ERROR);
			}
			
		};
	}

	@After
	public void tearDown() throws Exception {
		nativeConnector.disconnect();
		driver.stop();
//		sim.stop();
	}
	
	private String checkPcscTag(UnsignedInteger tag, UnsignedInteger expectedResponseCode, UnsignedInteger expectedLength) throws Exception{
		String result = driver.sendData(new UnsignedInteger(0),
				NativeDriverInterface.PCSC_FUNCTION_GET_CAPABILITIES,
				tag.getAsByteArray(), expectedLength.getAsByteArray());

		String expected = "";
		
		switch (expectedResponseCode.getAsInt()){
		case PcscConstants.VALUE_IFD_ERROR_TAG:
			expected = PcscConstants.IFD_ERROR_TAG.getAsHexString();
			break;
		case PcscConstants.VALUE_IFD_SUCCESS:
			expected = expectedResponseCode.getAsHexString() + NativeDriverInterface.MESSAGE_DIVIDER;
			break;
			default:
				fail("invalid expected response code");
		}

		assertTrue(result.startsWith(expected));
		return expected;
	}
	
	@Test
	public void testPcscGetCapabilitiesVendorName() throws Exception {
		checkPcscTag(PcscConstants.TAG_VENDOR_NAME, PcscConstants.IFD_SUCCESS, UnsignedInteger.MAX_VALUE);
	}

	@Test
	public void testPcscGetCapabilitiesSimultaneousAccess() throws Exception {
		checkPcscTag(PcscConstants.TAG_IFD_SIMULTANEOUS_ACCESS, PcscConstants.IFD_SUCCESS, UnsignedInteger.MAX_VALUE);
	}

	@Test
	public void testPcscGetCapabilitiesSlotsNumber() throws Exception {
		checkPcscTag(PcscConstants.TAG_IFD_SLOTS_NUMBER, PcscConstants.IFD_SUCCESS, UnsignedInteger.MAX_VALUE);
	}

	@Test
	public void testPcscGetCapabilitiesSlotThreadSafe() throws Exception {
		checkPcscTag(PcscConstants.TAG_IFD_SLOT_THREAD_SAFE, PcscConstants.IFD_SUCCESS, UnsignedInteger.MAX_VALUE);
	}

	@Test
	public void testPcscGetCapabilitiesNotExisting() throws Exception {
		checkPcscTag(new UnsignedInteger(0xFFFFFFFFl), PcscConstants.IFD_ERROR_TAG, UnsignedInteger.MAX_VALUE);
	}
	
	@Test
	public void testPcscTransmitToIcc() throws IOException, InterruptedException{
		final byte [] apdu = new byte []{0,0,0,0};
		//prepare the mock
//		sim.setHandler(handler);
		new NonStrictExpectations() {
			{
				Activator.getSim();
				result = simulator;
//				handler.processCommand(withPrefix("FF01"));
//				result = testAtr;
//				handler.processCommand(withEqual(HexString.encode(apdu)));
//				result = HexString.encode("RESPONSE".getBytes());
			}
		};
		
		String response = driver.sendData(new UnsignedInteger(0), NativeDriverInterface.PCSC_FUNCTION_POWER_ICC, PcscConstants.IFD_POWER_UP.getAsByteArray(), UnsignedInteger.MAX_VALUE.getAsByteArray());
		
		response = driver.sendData(new UnsignedInteger(0), NativeDriverInterface.PCSC_FUNCTION_TRANSMIT_TO_ICC, apdu, UnsignedInteger.MAX_VALUE.getAsByteArray());
		String expected = PcscConstants.IFD_SUCCESS.getAsHexString() + NativeDriverInterface.MESSAGE_DIVIDER + HexString.encode("RESPONSE".getBytes());
		assertEquals(expected, response);
	}
	
	@Test
	public void testPcscControl() throws IOException, InterruptedException{
		
		//prepare the mock
//		sim.setHandler(handler);
		new NonStrictExpectations() {
			{
				Activator.getSim();
				result = simulator;
//				handler.processCommand(withPrefix("FF01"));
//				result = "11223344";
			}
		};
		
		byte fakeTag = (byte) 255;
		UnsignedInteger fakeControlCode = new UnsignedInteger(1357);
		
		nativeConnector.addListener(new AbstractPcscFeature(fakeControlCode, fakeTag) {
			
			@Override
			public PcscCallResult processPcscCall(PcscCallData data) {
				return null;
			}
		});
		String response = driver.sendData(new UnsignedInteger(0), NativeDriverInterface.PCSC_FUNCTION_POWER_ICC, PcscConstants.IFD_POWER_UP.getAsByteArray(), UnsignedInteger.MAX_VALUE.getAsByteArray());
		
		response = driver.sendData(new UnsignedInteger(0), NativeDriverInterface.PCSC_FUNCTION_DEVICE_CONTROL, PcscConstants.CONTROL_CODE_GET_FEATURE_REQUEST.getAsByteArray(), new byte[]{}, UnsignedInteger.MAX_VALUE.getAsByteArray());
		
		//a string of tag|00000004|xxxxxxxx bytes is expected in all but the last 4 byte
		byte [] responseData = HexString.toByteArray(response.split(Pattern.quote(NativeDriverInterface.MESSAGE_DIVIDER))[1]);
		boolean foundFeature = false;
		for (int i = 0; i < responseData.length / 4; i++){
			assertEquals(4, responseData[i*4+1]);
			if (responseData[i*4] == fakeTag){
				Arrays.equals(fakeControlCode.getAsByteArray(), Arrays.copyOfRange(responseData, i*4+2, i*4+6));
				foundFeature = true;
			}
		}
		assertTrue("Feature inserted for testing not found", foundFeature);
		assertEquals(PcscConstants.IFD_SUCCESS.getAsHexString(), response.split(Pattern.quote(NativeDriverInterface.MESSAGE_DIVIDER))[0]);
	}

	@Test
	public void testPcscPowerIccPowerOn() throws Exception{
		final byte [] testAtr = "TESTATR".getBytes();

		//prepare the mock
//		sim.setHandler(handler);
		new NonStrictExpectations() {
			{
				Activator.getSim();
				result = simulator;
//				handler.processCommand(withPrefix("FF01"));
//				result = HexString.encode(testAtr);
			}
		};
		
		String result = driver.sendData(new UnsignedInteger(0), NativeDriverInterface.PCSC_FUNCTION_POWER_ICC, PcscConstants.IFD_POWER_UP.getAsByteArray(), UnsignedInteger.MAX_VALUE.getAsByteArray());
		
		String expected = PcscConstants.IFD_SUCCESS.getAsHexString() + NativeDriverInterface.MESSAGE_DIVIDER + HexString.encode(testAtr);
		assertEquals(expected, result);
	}

	@Test
	public void testPcscIsIccPresent() throws Exception{

		//prepare the mock
//		sim.setHandler(handler);
		new NonStrictExpectations() {
			{
				Activator.getSim();
				result = simulator;
//				handler.processCommand(withPrefix("FF90"));
//				result = "9000";
			}
		};
		
		String result = driver.sendData(new UnsignedInteger(0), NativeDriverInterface.PCSC_FUNCTION_IS_ICC_PRESENT, UnsignedInteger.MAX_VALUE.getAsByteArray());
		
		String expected = PcscConstants.IFD_ICC_PRESENT.getAsHexString();
		assertEquals(expected, result);
	}
	
	@Test
	public void testPcscPowerIccPowerDownWithoutPowerUp() throws Exception {
		
		new NonStrictExpectations() {
			{
				Activator.getSim();
				result = simulator;
			}
		};
		
		String result = driver.sendData(new UnsignedInteger(0), NativeDriverInterface.PCSC_FUNCTION_POWER_ICC, PcscConstants.IFD_POWER_DOWN.getAsByteArray(), UnsignedInteger.MAX_VALUE.getAsByteArray());
		
		String expected = PcscConstants.IFD_SUCCESS.getAsHexString();
		assertEquals(expected, result);
	}
	
	@Test
	public void testPcscPowerIccPowerDown() throws Exception{
		//prepare the mock
//		sim.setHandler(handler);

		new NonStrictExpectations() {
			{
				Activator.getSim();
				result = simulator;
//				handler.processCommand(withPrefix("FF01"));
//				result = HexString.encode(testAtr);
//				handler.processCommand(withPrefix("FF00"));
//				result = "9000";
			}
		};
		
		String result = driver.sendData(new UnsignedInteger(0), NativeDriverInterface.PCSC_FUNCTION_POWER_ICC, PcscConstants.IFD_POWER_UP.getAsByteArray(), UnsignedInteger.MAX_VALUE.getAsByteArray());
		
		result = driver.sendData(new UnsignedInteger(0), NativeDriverInterface.PCSC_FUNCTION_POWER_ICC, PcscConstants.IFD_POWER_DOWN.getAsByteArray(), UnsignedInteger.MIN_VALUE.getAsByteArray());

		String expected = PcscConstants.IFD_SUCCESS.getAsHexString();
		assertEquals(expected, result);
	}
	
	@Test
	public void testPcscPowerIccReset() throws Exception{
		final byte [] testAtr = "TESTATR".getBytes();

		//prepare the mock
//		sim.setHandler(handler);
		new NonStrictExpectations() {
			{
				Activator.getSim();
				result = simulator;
//				handler.processCommand(withPrefix("FF01"));
//				result = HexString.encode(testAtr);
//				handler.processCommand(withPrefix("FFFF"));
//				result = HexString.encode(testAtr);
			}
		};

		String result = driver.sendData(new UnsignedInteger(0), NativeDriverInterface.PCSC_FUNCTION_POWER_ICC, PcscConstants.IFD_POWER_UP.getAsByteArray(), UnsignedInteger.MAX_VALUE.getAsByteArray());
		
		result = driver.sendData(new UnsignedInteger(0), NativeDriverInterface.PCSC_FUNCTION_POWER_ICC, PcscConstants.IFD_RESET.getAsByteArray(), UnsignedInteger.MAX_VALUE.getAsByteArray());
		
		String expected = PcscConstants.IFD_SUCCESS.getAsHexString() + NativeDriverInterface.MESSAGE_DIVIDER + HexString.encode(testAtr);
		assertEquals(expected, result);
	}
	
	/**
	 * Checks if the listeners are processed in the correct order
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testListenerProcessingOrder() throws IOException, InterruptedException {

		
		nativeConnector.addListener(new PcscListener() {
			
			@Override
			public PcscCallResult processPcscCall(PcscCallData data) {
				assertEquals(0, data.getFunction().getAsSignedLong());
				data.setFunction(new UnsignedInteger(1));
				return null;
			}
		});
		nativeConnector.addListener(new PcscListener() {
			
			@Override
			public PcscCallResult processPcscCall(PcscCallData data) {
				assertEquals(1, data.getFunction().getAsSignedLong());
				return null;
			}
		});
		
		String result = driver.sendData(new UnsignedInteger(0), new UnsignedInteger(0));
		// this checks, if the native connector was the last listener to process because both listeners added in this testcase do not return an answer
		assertEquals(result, PcscConstants.IFD_NOT_SUPPORTED.getAsHexString());
	}
	
	/**
	 * Checks if an answer by a listener stops processing
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testListenerProcessingOrderEarlyAnswer() throws IOException, InterruptedException {
		
//		Deencapsulation.setField(de.persosim.driver.connector.Activator.class, "simulatorServiceTracker", simulatorServiceTracker);
		
		nativeConnector.addListener(new PcscListener() {
			
			@Override
			public PcscCallResult processPcscCall(PcscCallData data) {
				return new SimplePcscCallResult(PcscConstants.IFD_SUCCESS);
			}
		});
		
		String result = driver.sendData(new UnsignedInteger(0), new UnsignedInteger(0));
		assertEquals(result, PcscConstants.IFD_SUCCESS.getAsHexString());
	}
}
