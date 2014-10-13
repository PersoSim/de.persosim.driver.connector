package de.persosim.driver.connector;

import static org.junit.Assert.*;

import org.junit.Test;

import de.persosim.driver.test.ConnectorTest;

public class UnsignedIntegerTest extends ConnectorTest {

	final long MAX_LONG = 0x0FFFFFFFFl;
	final int MAX_INT = (int) MAX_LONG;
	
	@Test
	public void testConstructorLong() {
		long value = 12342134;
		UnsignedInteger uint = new UnsignedInteger(value);
		assertEquals(value, uint.getAsSignedLong());
	}
	
	@Test(expected = NumberFormatException.class)
	public void testConstructorLongNegative() {
		long value = -5;
		UnsignedInteger uint = new UnsignedInteger(value);
		assertEquals(value, uint.getAsSignedLong());
	}

	@Test
	public void testConstructorInt() {
		int value = 12342134;
		UnsignedInteger uint = new UnsignedInteger(value);
		assertEquals(value, uint.getAsSignedLong());
	}

	@Test
	public void testConstructorIntMaxValue() {
		long value = 0x0FFFFFFFFl;
		UnsignedInteger uint = new UnsignedInteger(value);
		assertEquals(value, uint.getAsSignedLong());
	}

	@Test(expected = NumberFormatException.class)
	public void testConstructorIntOverMax() {
		long value = 0x01FFFFFFFFl;
		UnsignedInteger uint = new UnsignedInteger(value);
		assertEquals(value, uint.getAsSignedLong());
	}

	@Test
	public void testConstructorArray() {
		byte[] value = new byte[] { 0x12, 0x34, 0x56, 0x78 };
		UnsignedInteger uint = new UnsignedInteger(value);
		assertEquals(0x12345678, uint.getAsSignedLong());
	}

	@Test(expected = NumberFormatException.class)
	public void testConstructorArrayTooLong() {
		byte[] value = new byte[] { 0x12, 0x34, 0x56, 0x78, (byte) 0x89 };
		new UnsignedInteger(value);
	}
	
	@Test
	public void testGetAsHexString(){
		assertEquals("FFFFFFFF", new UnsignedInteger(MAX_INT).getAsHexString());
	}
	
	@Test
	public void testGetAsInt(){
		assertEquals(MAX_INT, new UnsignedInteger(MAX_INT).getAsInt());
	}
	
	@Test
	public void testGetAsLong(){
		assertEquals(MAX_LONG, new UnsignedInteger(MAX_INT).getAsSignedLong());
	}
	
	@Test
	public void testGetAsByteArray(){
		assertArrayEquals(new byte [] {0x12,0x34,0x56,0x78}, new UnsignedInteger(0x12345678).getAsByteArray());
	}
}
