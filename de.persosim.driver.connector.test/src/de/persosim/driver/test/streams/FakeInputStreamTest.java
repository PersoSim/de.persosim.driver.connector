package de.persosim.driver.test.streams;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.persosim.driver.test.ConnectorTest;

public class FakeInputStreamTest extends ConnectorTest {

	byte[] testData;
	InputStream stream;

	@Before
	public void setUp() {
		testData = new byte[] { 1, 2, 3, 4, 5 };
		stream = new FakeInputStream(testData);
	}
	
	@After
	public void tearDown() throws IOException{
		stream.close();
	}

	/**
	 * Positive test case: read the stream byte by byte
	 * @throws IOException
	 */
	@Test
	public void testReadSimple() throws IOException {
		for (int i = 0; i < testData.length; i++){
			assertEquals(testData[i], stream.read());
		}
		assertEquals(-1, stream.read());
	}

	/**
	 * Positive test case: read the stream all at once
	 * @throws IOException
	 */
	@Test
	public void testReadComplete() throws IOException{
		byte [] received = new byte [testData.length];
		int readNumber = stream.read(received);
		assertEquals(testData.length, readNumber);
		assertArrayEquals(testData, received);
	}
}
