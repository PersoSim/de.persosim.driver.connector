package de.persosim.driver.connector.pcsc;

import static org.junit.Assert.*;

import java.security.InvalidParameterException;

import org.junit.Test;

import de.persosim.driver.test.ConnectorTest;
import de.persosim.simulator.utils.HexString;

public class PcscCallDataTest extends ConnectorTest {
	
	/**
	 * Positive test using valid data
	 */
	@Test
	public void testConstructor(){
		PcscCallData data = new PcscCallData("00000001|00000002|000000000003|000004");
		assertEquals("00000001", data.getFunction().getAsHexString());
		assertEquals("00000002", data.getLogicalUnitNumber().getAsHexString());
		assertEquals(2, data.getParameters().size());
		assertEquals("000000000003", HexString.encode(data.getParameters().get(0)));
		assertEquals("000004", HexString.encode(data.getParameters().get(1)));
	}
	
	/**
	 * Negative test with a missing lun
	 */
	@Test(expected=InvalidParameterException.class)
	public void testConstructorNoLunNoData(){
		new PcscCallData("00000001");
	}
	
	/**
	 * Negative test with empty input string
	 */
	@Test(expected=InvalidParameterException.class)
	public void testConstructorEmptyInput(){
		new PcscCallData("");
	}
	
	/**
	 * Positive test using no data
	 */
	@Test
	public void testConstructorNoData(){
		PcscCallData data = new PcscCallData("00000001|00000002");
		assertEquals("00000001", data.getFunction().getAsHexString());
		assertEquals("00000002", data.getLogicalUnitNumber().getAsHexString());
		assertEquals(0, data.getParameters().size());
	}
	
	/**
	 * Positive test using short function parameter
	 */
	@Test
	public void testConstructorNoDataShortFunction(){
		PcscCallData data = new PcscCallData("01|00000002");
		assertEquals("00000001", data.getFunction().getAsHexString());
		assertEquals("00000002", data.getLogicalUnitNumber().getAsHexString());
		assertEquals(0, data.getParameters().size());
	}
	
	/**
	 * Positive test using short lun parameter
	 */
	@Test
	public void testConstructorNoDataShortLun(){
		PcscCallData data = new PcscCallData("00000001|02");
		assertEquals("00000001", data.getFunction().getAsHexString());
		assertEquals("00000002", data.getLogicalUnitNumber().getAsHexString());
		assertEquals(0, data.getParameters().size());
	}
	
	/**
	 * Negative test using too long function number
	 */
	@Test(expected=NumberFormatException.class)
	public void testConstructorFunctionToBig(){
		new PcscCallData("01FFFFFFFF|00000002");
	}
	
	/**
	 * Negative test using too long lun
	 */
	@Test(expected=NumberFormatException.class)
	public void testConstructorLunToBig(){
		new PcscCallData("00000001|01FFFFFFFF");
	}
}
