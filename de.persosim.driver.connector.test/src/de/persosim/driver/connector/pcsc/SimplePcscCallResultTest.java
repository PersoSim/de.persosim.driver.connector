package de.persosim.driver.connector.pcsc;

import static org.junit.Assert.*;

import org.junit.Test;

import de.persosim.driver.connector.UnsignedInteger;
import de.persosim.driver.test.ConnectorTest;

public class SimplePcscCallResultTest extends ConnectorTest {

	@Test
	public void testConstructorStatusCode(){
		assertEquals("00000000", new SimplePcscCallResult(new UnsignedInteger(0)).getEncoded());
	}
	
	@Test
	public void testConstructorStatusCodeAndData(){
		assertEquals("00000000|01|02", new SimplePcscCallResult(new UnsignedInteger(0), new byte [] {1}, new byte [] {2}).getEncoded());
	}
}
