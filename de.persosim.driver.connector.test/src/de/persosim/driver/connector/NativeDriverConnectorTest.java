package de.persosim.driver.connector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.security.Security;

import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

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
				kernel.init();
			}
		};
		
		sim = new SocketSimulator(new DefaultPersonalization() {
			
			@Override
			protected void addTaTrustPoints() throws CertificateNotParseableException {
				// TODO Auto-generated method stub
				
			}
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

	@Test
	public void testPcscGetCapabilitiesVendorName() throws Exception {
		// FIXME find better solution for timing issues while testing
		Thread.sleep(100);
		String tag = "00000100";

		String result = driver.sendData(0,
				NativeDriverComm.PCSC_FUNCTION_GET_CAPABILITIES,
				HexString.toByteArray(tag));

		String expected = PcscConstants.IFD_SUCCESS + "#" + tag;

		assertTrue(result.indexOf(expected) == 0);
	}

	@Test
	public void testPcscGetCapabilitiesNotExisting() throws Exception {
		// FIXME find better solution for timing issues while testing
		Thread.sleep(100);
		String tag = "FFFFFFFF";

		String result = driver.sendData(0,
				NativeDriverComm.PCSC_FUNCTION_GET_CAPABILITIES,
				HexString.toByteArray(tag));

		String expected = PcscConstants.IFD_SUCCESS + "#" + tag;

		assertTrue(!(result.indexOf(expected) == 0));
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
	public void testPcscPowerIccPowerDown() throws Exception{
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
		
		String expected = "" + PcscConstants.IFD_SUCCESS + "#" + HexString.encode(PcscDataHelper.buildTlv(Utils.toUnsignedByteArray(PcscConstants.TAG_IFD_ATR), new byte [0]));
		assertEquals(expected, result);
	}
}
