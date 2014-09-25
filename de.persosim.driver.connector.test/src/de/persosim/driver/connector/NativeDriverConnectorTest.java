package de.persosim.driver.connector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.Security;
import java.util.Arrays;
import java.util.regex.Pattern;

import mockit.Expectations;
import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.persosim.driver.connector.pcsc.AbstractPcscFeature;
import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.connector.pcsc.PcscCallResult;
import de.persosim.driver.connector.pcsc.PcscConstants;
import de.persosim.driver.test.TestDriver;
import de.persosim.simulator.SocketSimulator;
import de.persosim.simulator.exception.CertificateNotParseableException;
import de.persosim.simulator.perso.DefaultPersonalization;
import de.persosim.simulator.platform.PersoSimKernel;
import de.persosim.simulator.utils.HexString;
import de.persosim.simulator.utils.PersoSimLogger;

public class NativeDriverConnectorTest {

	private NativeDriverConnector nativeConnector;
	private TestDriver driver;

	@Mocked
	PersoSimKernel kernel;
	SocketSimulator sim;

	@BeforeClass
	public static void setUpClass() {
		//setup logging
		PersoSimLogger.init();
		
		//register BouncyCastle provider
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}
	
	@Before
	public void setUp() throws Exception {
		new NonStrictExpectations(PersoSimKernel.class){
			{
				kernel.init(); // TODO this method seems to be incorrectly mocked and executed
			}
		};
		
		sim = new SocketSimulator(new DefaultPersonalization() {
			@Override
			protected void addTaTrustPoints() throws CertificateNotParseableException {}
		}, 9876);
		sim.start();
		
		driver = new TestDriver();
		driver.start();
				
		nativeConnector = new NativeDriverConnector("localhost",
				TestDriver.PORT_NUMBER_DEFAULT, "localhost", 9876);
		nativeConnector.connect();
	}

	@After
	public void tearDown() throws Exception {
		nativeConnector.disconnect();
		driver.stop();
		sim.stop();
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
		// XXX find better solution for timing issues while testing
		Thread.sleep(100);
		checkPcscTag(PcscConstants.TAG_VENDOR_NAME, PcscConstants.IFD_SUCCESS, UnsignedInteger.MAX_VALUE);
	}

	@Test
	public void testPcscGetCapabilitiesSimultaneousAccess() throws Exception {
		// XXX find better solution for timing issues while testing
		Thread.sleep(100);
		checkPcscTag(PcscConstants.TAG_IFD_SIMULTANEOUS_ACCESS, PcscConstants.IFD_SUCCESS, UnsignedInteger.MAX_VALUE);
	}

	@Test
	public void testPcscGetCapabilitiesSlotsNumber() throws Exception {
		// XXX find better solution for timing issues while testing
		Thread.sleep(100);
		checkPcscTag(PcscConstants.TAG_IFD_SLOTS_NUMBER, PcscConstants.IFD_SUCCESS, UnsignedInteger.MAX_VALUE);
	}

	@Test
	public void testPcscGetCapabilitiesSlotThreadSafe() throws Exception {
		// XXX find better solution for timing issues while testing
		Thread.sleep(100);
		checkPcscTag(PcscConstants.TAG_IFD_SLOT_THREAD_SAFE, PcscConstants.IFD_SUCCESS, UnsignedInteger.MAX_VALUE);
	}

	@Test
	public void testPcscGetCapabilitiesNotExisting() throws Exception {
		// XXX find better solution for timing issues while testing
		Thread.sleep(100);
		checkPcscTag(new UnsignedInteger(0xFFFFFFFFl), PcscConstants.IFD_ERROR_TAG, UnsignedInteger.MAX_VALUE);
	}
	
	@Test
	public void testPcscTransmitToIcc() throws IOException, InterruptedException{
		// XXX find better solution for timing issues while testing
		Thread.sleep(100);
		final byte [] apdu = new byte []{0,0,0,0};

		new Expectations(PersoSimKernel.class){
			{
				kernel.powerOn();
				result = new byte [0];
				kernel.process(apdu);
				result = "RESPONSE".getBytes();
			}
		};
		
		String response = driver.sendData(new UnsignedInteger(0), NativeDriverInterface.PCSC_FUNCTION_POWER_ICC, PcscConstants.IFD_POWER_UP.getAsByteArray(), UnsignedInteger.MAX_VALUE.getAsByteArray());
		
		response = driver.sendData(new UnsignedInteger(0), NativeDriverInterface.PCSC_FUNCTION_TRANSMIT_TO_ICC, apdu, UnsignedInteger.MAX_VALUE.getAsByteArray());
		String expected = PcscConstants.IFD_SUCCESS.getAsHexString() + NativeDriverInterface.MESSAGE_DIVIDER + HexString.encode("RESPONSE".getBytes());
		assertEquals(expected, response);
	}
	
	@Test
	public void testPcscControl() throws IOException, InterruptedException{
		// XXX find better solution for timing issues while testing
		Thread.sleep(100);

		new Expectations(PersoSimKernel.class){
			{
				kernel.powerOn();
				result = new byte [0];
			}
		};
		
		byte fakeTag = (byte) 255;
		UnsignedInteger fakeControlCode = new UnsignedInteger(1357);
		
		nativeConnector.addListener(new AbstractPcscFeature(fakeControlCode, fakeTag) {
			
			@Override
			public PcscCallResult processPcscCall(PcscCallData data) {
				return null;
			}
			
			@Override
			public byte[] getCapabilities() {
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
		//prepare the mock
		final byte [] testAtr = "TESTATR".getBytes();
		
		new NonStrictExpectations(PersoSimKernel.class){
			{
				kernel.powerOn();
				result = testAtr;
			}
		};
		
		//XXX find better solution for timing issues while testing
		Thread.sleep(100);
		
		String result = driver.sendData(new UnsignedInteger(0), NativeDriverInterface.PCSC_FUNCTION_POWER_ICC, PcscConstants.IFD_POWER_UP.getAsByteArray(), UnsignedInteger.MAX_VALUE.getAsByteArray());
		
		String expected = PcscConstants.IFD_SUCCESS.getAsHexString() + NativeDriverInterface.MESSAGE_DIVIDER + HexString.encode(testAtr);
		assertEquals(expected, result);
	}

	@Test
	public void testPcscIsIccPresent() throws Exception{
		
		//XXX find better solution for timing issues while testing
		Thread.sleep(100);
		
		String result = driver.sendData(new UnsignedInteger(0), NativeDriverInterface.PCSC_FUNCTION_IS_ICC_PRESENT, UnsignedInteger.MAX_VALUE.getAsByteArray());
		
		String expected = PcscConstants.IFD_ICC_PRESENT.getAsHexString();
		assertEquals(expected, result);
	}
	
	@Test
	public void testPcscPowerIccPowerDownWithoutPowerUp() throws Exception{
		//prepare the mock
		
		new NonStrictExpectations(PersoSimKernel.class){
			{
				kernel.powerOff();
				result = new byte [] {(byte) 0x90, 0};
			}
		};
		
		//XXX find better solution for timing issues while testing
		Thread.sleep(100);
		
		String result = driver.sendData(new UnsignedInteger(0), NativeDriverInterface.PCSC_FUNCTION_POWER_ICC, PcscConstants.IFD_POWER_DOWN.getAsByteArray(), UnsignedInteger.MAX_VALUE.getAsByteArray());
		
		String expected = PcscConstants.IFD_ERROR_POWER_ACTION.getAsHexString();
		assertEquals(expected, result);
	}
	
	@Test
	public void testPcscPowerIccPowerDown() throws Exception{
		//prepare the mock
		final byte [] testAtr = "TESTATR".getBytes();
		
		new NonStrictExpectations(PersoSimKernel.class){
			{
				kernel.powerOn();
				result = testAtr;
				kernel.powerOff();
				result = new byte [] {(byte) 0x90, 0};
			}
		};
		
		//XXX find better solution for timing issues while testing
		Thread.sleep(100);
		
		String result = driver.sendData(new UnsignedInteger(0), NativeDriverInterface.PCSC_FUNCTION_POWER_ICC, PcscConstants.IFD_POWER_UP.getAsByteArray(), UnsignedInteger.MAX_VALUE.getAsByteArray());
		
		result = driver.sendData(new UnsignedInteger(0), NativeDriverInterface.PCSC_FUNCTION_POWER_ICC, PcscConstants.IFD_POWER_DOWN.getAsByteArray(), UnsignedInteger.MIN_VALUE.getAsByteArray());

		String expected = PcscConstants.IFD_SUCCESS.getAsHexString();
		assertEquals(expected, result);
	}
	
	@Test
	public void testPcscPowerIccReset() throws Exception{
		
		//prepare the mock
		final byte [] testAtr = "TESTATR".getBytes();
		
		new NonStrictExpectations(PersoSimKernel.class){
			{
				kernel.powerOn();
				result = testAtr;
				kernel.reset();
				result = testAtr;
			}
		};
		
		//XXX find better solution for timing issues while testing
		Thread.sleep(100);

		String result = driver.sendData(new UnsignedInteger(0), NativeDriverInterface.PCSC_FUNCTION_POWER_ICC, PcscConstants.IFD_POWER_UP.getAsByteArray(), UnsignedInteger.MAX_VALUE.getAsByteArray());
		
		result = driver.sendData(new UnsignedInteger(0), NativeDriverInterface.PCSC_FUNCTION_POWER_ICC, PcscConstants.IFD_RESET.getAsByteArray(), UnsignedInteger.MAX_VALUE.getAsByteArray());
		
		String expected = PcscConstants.IFD_SUCCESS.getAsHexString() + NativeDriverInterface.MESSAGE_DIVIDER + HexString.encode(testAtr);
		assertEquals(expected, result);
	}
}
