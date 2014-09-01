package de.persosim.driver.connector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.Security;

import mockit.Expectations;
import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.persosim.driver.connector.pcsc.PcscConstants;
import de.persosim.driver.connector.pcsc.PcscDataHelper;
import de.persosim.driver.test.TestDriver;
import de.persosim.simulator.SocketSimulator;
import de.persosim.simulator.exception.CertificateNotParseableException;
import de.persosim.simulator.perso.DefaultPersonalization;
import de.persosim.simulator.platform.PersoSimKernel;
import de.persosim.simulator.utils.HexString;
import de.persosim.simulator.utils.PersoSimLogger;
import de.persosim.simulator.utils.Utils;

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

	private String checkPcscTag(byte [] tag, int expectedResponseCode) throws Exception{
		String result = driver.sendData(0,
				NativeDriverComm.PCSC_FUNCTION_GET_CAPABILITIES,
				tag);

		String expected = expectedResponseCode + "#" + HexString.encode(tag);
		
		switch (expectedResponseCode){
		case PcscConstants.IFD_ERROR_TAG:
			expected = PcscConstants.IFD_ERROR_TAG + "";
			break;
		case PcscConstants.IFD_SUCCESS:
			expected = expectedResponseCode + "#" + HexString.encode(tag);
			break;
			default:
				fail("invalid expected response code");
		}

		assertTrue(result.startsWith(expected));
		return expected;
	}
	
	@Test
	public void testPcscGetCapabilitiesVendorName() throws Exception {
		// FIXME find better solution for timing issues while testing
		Thread.sleep(100);
		checkPcscTag(Utils.toUnsignedByteArray(PcscConstants.TAG_VENDOR_NAME), PcscConstants.IFD_SUCCESS);
	}

	@Test
	public void testPcscGetCapabilitiesSimultaneousAccess() throws Exception {
		// FIXME find better solution for timing issues while testing
		Thread.sleep(100);
		checkPcscTag(Utils.toUnsignedByteArray(PcscConstants.TAG_IFD_SIMULTANEOUS_ACCESS), PcscConstants.IFD_SUCCESS);
	}

	@Test
	public void testPcscGetCapabilitiesSlotsNumber() throws Exception {
		// FIXME find better solution for timing issues while testing
		Thread.sleep(100);
		checkPcscTag(Utils.toUnsignedByteArray(PcscConstants.TAG_IFD_SLOTS_NUMBER), PcscConstants.IFD_SUCCESS);
	}

	@Test
	public void testPcscGetCapabilitiesSlotThreadSafe() throws Exception {
		// FIXME find better solution for timing issues while testing
		Thread.sleep(100);
		checkPcscTag(Utils.toUnsignedByteArray(PcscConstants.TAG_IFD_SLOT_THREAD_SAFE), PcscConstants.IFD_SUCCESS);
	}

	@Test
	public void testPcscGetCapabilitiesNotExisting() throws Exception {
		// FIXME find better solution for timing issues while testing
		Thread.sleep(100);
		checkPcscTag(HexString.toByteArray("FFFFFFFF"), PcscConstants.IFD_ERROR_TAG);
	}
	
	@Test
	public void testPcscTransmitToIcc() throws IOException, InterruptedException{
		// FIXME find better solution for timing issues while testing
		Thread.sleep(100);
		final byte [] apdu = new byte []{0,0,0,0};
		byte [] testData = Utils.concatByteArrays(Utils.toUnsignedByteArray(PcscConstants.SCARD_PROTOCOL_T0), Utils.toUnsignedByteArray(8), apdu);

		new Expectations(PersoSimKernel.class){
			{
				kernel.powerOn();
				result = new byte [0];
				kernel.process(apdu);
				result = "RESPONSE".getBytes();
			}
		};
		
		String response = driver.sendData(0, NativeDriverComm.PCSC_FUNCTION_POWER_ICC, Utils.toUnsignedByteArray((short)PcscConstants.IFD_POWER_UP));
		
		response = driver.sendData(0, NativeDriverComm.PCSC_FUNCTION_TRANSMIT_TO_ICC, testData);
		String expected = PcscConstants.IFD_SUCCESS + "#" + HexString.encode("RESPONSE".getBytes());
		assertEquals(expected, response);
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
		
		//FIXME find better solution for timing issues while testing
		Thread.sleep(100);
		
		String result = driver.sendData(0, NativeDriverComm.PCSC_FUNCTION_POWER_ICC, Utils.toUnsignedByteArray((short)PcscConstants.IFD_POWER_UP));
		
		String expected = "" + PcscConstants.IFD_SUCCESS + "#" + HexString.encode(PcscDataHelper.buildTlv(Utils.toUnsignedByteArray(PcscConstants.TAG_IFD_ATR), testAtr));
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
		
		//FIXME find better solution for timing issues while testing
		Thread.sleep(100);
		
		String result = driver.sendData(0, NativeDriverComm.PCSC_FUNCTION_POWER_ICC, Utils.toUnsignedByteArray((short)PcscConstants.IFD_POWER_DOWN));
		
		String expected = "" + PcscConstants.IFD_ERROR_POWER_ACTION;
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
		
		//FIXME find better solution for timing issues while testing
		Thread.sleep(100);
		
		String result = driver.sendData(0, NativeDriverComm.PCSC_FUNCTION_POWER_ICC, Utils.toUnsignedByteArray((short)PcscConstants.IFD_POWER_UP));
		
		result = driver.sendData(0, NativeDriverComm.PCSC_FUNCTION_POWER_ICC, Utils.toUnsignedByteArray((short)PcscConstants.IFD_POWER_DOWN));

		String expected = "" + PcscConstants.IFD_SUCCESS;
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
		
		//FIXME find better solution for timing issues while testing
		Thread.sleep(100);

		String result = driver.sendData(0, NativeDriverComm.PCSC_FUNCTION_POWER_ICC, Utils.toUnsignedByteArray((short)PcscConstants.IFD_POWER_UP));
		
		result = driver.sendData(0, NativeDriverComm.PCSC_FUNCTION_POWER_ICC, Utils.toUnsignedByteArray((short)PcscConstants.IFD_RESET));
		
		String expected = "" + PcscConstants.IFD_SUCCESS + "#" + HexString.encode(PcscDataHelper.buildTlv(Utils.toUnsignedByteArray(PcscConstants.TAG_IFD_ATR), testAtr));
		assertEquals(expected, result);
	}
}
