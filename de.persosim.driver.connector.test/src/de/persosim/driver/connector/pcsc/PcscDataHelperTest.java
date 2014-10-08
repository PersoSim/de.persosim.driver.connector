package de.persosim.driver.connector.pcsc;

import static org.junit.Assert.*;

import org.junit.Test;

import de.persosim.driver.test.ConnectorTest;

public class PcscDataHelperTest extends ConnectorTest {

	@Test
	public void testParseBoolTrue(){
		byte [] trueBool = new byte []{0,1};
		assertTrue(PcscDataHelper.parseBool(trueBool, 0));
	}

	@Test
	public void testParseBoolOffset(){
		byte [] trueBool = new byte []{1,2,3,0,1};
		assertTrue(PcscDataHelper.parseBool(trueBool, 3));
	}

	@Test
	public void testParseBoolFalse(){
		byte [] falseBool = new byte []{0,0};
		assertTrue(!PcscDataHelper.parseBool(falseBool, 0));
	}
}
