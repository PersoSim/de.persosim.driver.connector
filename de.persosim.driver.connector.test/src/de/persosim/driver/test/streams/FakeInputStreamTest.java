package de.persosim.driver.test.streams;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

import de.persosim.driver.test.ConnectorTest;

public class FakeInputStreamTest extends ConnectorTest {

	byte[] testData;

	@Before
	public void setUp() {
		testData = new byte[] { 1, 2, 3, 4, 5 };
	}

	/**
	 * Positive test case: read the stream byte by byte
	 * @throws IOException
	 */
	@Test
	public void testReadSimple() throws IOException {
		InputStream stream = new FakeInputStream(testData);
		for (int i = 0; i < testData.length; i++){
			assertEquals(testData[i], stream.read());
		}
		assertEquals(-1, stream.read());
		stream.close();
	}
}
