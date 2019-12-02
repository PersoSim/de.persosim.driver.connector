package de.persosim.driver.connector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.globaltester.simulator.Simulator;
import org.globaltester.simulator.SimulatorEventListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.persosim.driver.connector.features.DefaultListener;
import de.persosim.driver.connector.pcsc.AbstractPcscFeature;
import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.connector.pcsc.PcscCallResult;
import de.persosim.driver.connector.pcsc.PcscConstants;
import de.persosim.driver.connector.service.IfdConnector;
import de.persosim.driver.connector.service.IfdConnectorImpl;
import de.persosim.driver.test.ConnectorTest;
import de.persosim.simulator.platform.Iso7816;
import de.persosim.simulator.utils.HexString;
import de.persosim.simulator.utils.Utils;

/**
 * This class tests the {@link IfdConnectorImpl}.
 * @author mboonk
 *
 */
public class IfdConnectorTest extends ConnectorTest{

	private IfdConnector nativeConnector;
	private final byte [] testAtr = "TESTATR".getBytes();
	
	private Simulator simulator;
	private TestDriverComm testDriverComm;
	
	//IMPL use JUnit test rule with http://junit.org/apidocs/org/junit/rules/DisableOnDebug.html when using JUnit 4.12
	
	@Before
	public void setUp() throws Exception {
				
		nativeConnector = new IfdConnectorImpl();
		nativeConnector.addListener(new DefaultListener());
		
		testDriverComm = new TestDriverComm();
		nativeConnector.connect(testDriverComm);
		
		while (!nativeConnector.isRunning()) {
			Thread.sleep(100);
		}
		
		simulator = new Simulator() {
			
			@Override
			public boolean stopSimulator() {
				return true;
			}
			
			@Override
			public boolean startSimulator() {
				return true;
			}
			
			@Override
			public boolean restartSimulator() {
				return true;
			}
			
			@Override
			public byte[] processCommand(byte[] apdu) {
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
				return true;
			}
			
			@Override
			public byte[] cardReset() {
				return testAtr;
			}
			
			@Override
			public byte[] cardPowerUp() {
				return testAtr;
			}
			
			@Override
			public byte[] cardPowerDown() {
				return Utils.toUnsignedByteArray(Iso7816.SW_9000_NO_ERROR);
			}

			@Override
			public void addEventListener(SimulatorEventListener... newListeners) {
				// intentionally empty, no implementation needed in this test code		
			}

			@Override
			public void removeEventListener(SimulatorEventListener oldListener) {
				// intentionally empty, no implementation needed in this test code
			}
			
		};
	}

	@After
	public void tearDown() throws Exception {
		nativeConnector.disconnect();
	}
	
	List<byte []> getAsList(byte [] ... arrays){
		List<byte []> result = new LinkedList<>();
		for (byte [] array : arrays) {
			result.add(array);
		}
		return result;
	}
	
	private String checkPcscTag(UnsignedInteger tag, UnsignedInteger expectedResponseCode, UnsignedInteger expectedLength) throws Exception{

		PcscCallResult result = testDriverComm.sendData(new PcscCallData(IfdInterface.PCSC_FUNCTION_GET_CAPABILITIES, new UnsignedInteger(0), getAsList(tag.getAsByteArray(), expectedLength.getAsByteArray())));

		String expected = "";
		
		switch (expectedResponseCode.getAsInt()){
		case PcscConstants.VALUE_IFD_ERROR_TAG:
			expected = PcscConstants.IFD_ERROR_TAG.getAsHexString();
			break;
		case PcscConstants.VALUE_IFD_SUCCESS:
			expected = expectedResponseCode.getAsHexString() + IfdInterface.MESSAGE_DIVIDER;
			break;
			default:
				fail("invalid expected response code");
		}

		assertTrue(result.getEncoded().startsWith(expected));
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

		SimulatorManager.setSimulator(simulator);
		
		testDriverComm.sendData(new PcscCallData(IfdInterface.PCSC_FUNCTION_POWER_ICC, new UnsignedInteger(0), getAsList(PcscConstants.IFD_POWER_UP.getAsByteArray(), UnsignedInteger.MAX_VALUE.getAsByteArray())));
		PcscCallResult response = testDriverComm.sendData(new PcscCallData(IfdInterface.PCSC_FUNCTION_TRANSMIT_TO_ICC, new UnsignedInteger(0), getAsList(apdu, UnsignedInteger.MAX_VALUE.getAsByteArray())));
				
		String expected = PcscConstants.IFD_SUCCESS.getAsHexString() + IfdInterface.MESSAGE_DIVIDER + HexString.encode("RESPONSE".getBytes());
		assertEquals(expected, response.getEncoded());
	}
	
	@Test
	public void testPcscControl() throws IOException, InterruptedException{

		SimulatorManager.setSimulator(simulator);
		
		byte fakeTag = (byte) 255;
		UnsignedInteger fakeControlCode = new UnsignedInteger(1357);
		
		nativeConnector.addListener(new AbstractPcscFeature(fakeControlCode, fakeTag) {
			
			@Override
			public PcscCallResult processPcscCall(PcscCallData data) {
				return null;
			}
		});
		

		testDriverComm.sendData(new PcscCallData(IfdInterface.PCSC_FUNCTION_POWER_ICC, new UnsignedInteger(0), getAsList(PcscConstants.IFD_POWER_UP.getAsByteArray(), UnsignedInteger.MAX_VALUE.getAsByteArray())));

		PcscCallResult response = testDriverComm.sendData(new PcscCallData(IfdInterface.PCSC_FUNCTION_DEVICE_CONTROL, new UnsignedInteger(0), getAsList( PcscConstants.CONTROL_CODE_GET_FEATURE_REQUEST.getAsByteArray(), new byte[]{}, UnsignedInteger.MAX_VALUE.getAsByteArray())));
		
		//a string of tag|00000004|xxxxxxxx bytes is expected in all but the last 4 byte
		byte [] responseData = HexString.toByteArray(response.getEncoded().split(Pattern.quote(IfdInterface.MESSAGE_DIVIDER))[1]);
		boolean foundFeature = false;
		for (int i = 0; i < responseData.length / 4; i++){
			assertEquals(4, responseData[i*4+1]);
			if (responseData[i*4] == fakeTag){
				Arrays.equals(fakeControlCode.getAsByteArray(), Arrays.copyOfRange(responseData, i*4+2, i*4+6));
				foundFeature = true;
			}
		}
		assertTrue("Feature inserted for testing not found", foundFeature);
		assertEquals(PcscConstants.IFD_SUCCESS.getAsHexString(), response.getEncoded().split(Pattern.quote(IfdInterface.MESSAGE_DIVIDER))[0]);
	}

	@Test
	public void testPcscPowerIccPowerOn() throws Exception{
		final byte [] testAtr = "TESTATR".getBytes();

		SimulatorManager.setSimulator(simulator);

		PcscCallResult result = testDriverComm.sendData(new PcscCallData(IfdInterface.PCSC_FUNCTION_POWER_ICC, new UnsignedInteger(0), getAsList(PcscConstants.IFD_POWER_UP.getAsByteArray(), UnsignedInteger.MAX_VALUE.getAsByteArray())));
		
		String expected = PcscConstants.IFD_SUCCESS.getAsHexString() + IfdInterface.MESSAGE_DIVIDER + HexString.encode(testAtr);
		assertEquals(expected, result.getEncoded());
	}

	@Test
	public void testPcscIsIccPresent() throws Exception{

		SimulatorManager.setSimulator(simulator);

		PcscCallResult result = testDriverComm.sendData(new PcscCallData(IfdInterface.PCSC_FUNCTION_IS_ICC_PRESENT, new UnsignedInteger(0), getAsList(UnsignedInteger.MAX_VALUE.getAsByteArray())));
		
		String expected = PcscConstants.IFD_ICC_PRESENT.getAsHexString();
		assertEquals(expected, result.getEncoded());
	}
	
	@Test
	public void testPcscPowerIccPowerDownWithoutPowerUp() throws Exception {

		SimulatorManager.setSimulator(simulator);
		
		PcscCallResult result = testDriverComm.sendData(new PcscCallData(IfdInterface.PCSC_FUNCTION_POWER_ICC, new UnsignedInteger(0), getAsList(PcscConstants.IFD_POWER_DOWN.getAsByteArray(), UnsignedInteger.MAX_VALUE.getAsByteArray())));
		
		String expected = PcscConstants.IFD_SUCCESS.getAsHexString();
		assertEquals(expected, result.getEncoded());
	}
	
	@Test
	public void testPcscPowerIccPowerDown() throws Exception{
		SimulatorManager.setSimulator(simulator);
		
		testDriverComm.sendData(new PcscCallData(IfdInterface.PCSC_FUNCTION_POWER_ICC, new UnsignedInteger(0), getAsList(PcscConstants.IFD_POWER_UP.getAsByteArray(), UnsignedInteger.MAX_VALUE.getAsByteArray())));
		
		PcscCallResult result = testDriverComm.sendData(new PcscCallData(IfdInterface.PCSC_FUNCTION_POWER_ICC, new UnsignedInteger(0), getAsList(PcscConstants.IFD_POWER_DOWN.getAsByteArray(), UnsignedInteger.MAX_VALUE.getAsByteArray())));

		String expected = PcscConstants.IFD_SUCCESS.getAsHexString();
		assertEquals(expected, result.getEncoded());
	}
	
	@Test
	public void testPcscPowerIccReset() throws Exception{
		final byte [] testAtr = "TESTATR".getBytes();
		
		SimulatorManager.setSimulator(simulator);
		
		
		testDriverComm.sendData(new PcscCallData(IfdInterface.PCSC_FUNCTION_POWER_ICC, new UnsignedInteger(0), getAsList(PcscConstants.IFD_POWER_UP.getAsByteArray(), UnsignedInteger.MAX_VALUE.getAsByteArray())));
		
		PcscCallResult result = testDriverComm.sendData(new PcscCallData(IfdInterface.PCSC_FUNCTION_POWER_ICC, new UnsignedInteger(0), getAsList(PcscConstants.IFD_RESET.getAsByteArray(), UnsignedInteger.MAX_VALUE.getAsByteArray())));
		
		String expected = PcscConstants.IFD_SUCCESS.getAsHexString() + IfdInterface.MESSAGE_DIVIDER + HexString.encode(testAtr);
		assertEquals(expected, result.getEncoded());
	}
}
