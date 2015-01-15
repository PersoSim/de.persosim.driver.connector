package de.persosim.driver.test.streams;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import de.persosim.driver.test.ConnectorTest;
import de.persosim.simulator.utils.Utils;

/**
 * Test the {@link FakeOutputStream} class using several patterns focussing on
 * different operating system line feed styles.
 * 
 * @author mboonk
 *
 */
public class FakeOutputStreamTest extends ConnectorTest {
	byte[] cr;
	byte[] lf;
	byte[] crlf;

	@Before
	public void setUp() {
		cr = new byte[] { '\r' };
		lf = new byte[] { '\n' };
		crlf = Utils.concatByteArrays(cr, lf);
	}

	@Test
	public void testWriteSimple() throws IOException {
		FakeOutputStream stream = new FakeOutputStream(false);
		stream.write(5);
		byte[] writtenBytes = stream.getWrittenBytes();
		assertEquals(writtenBytes.length, 1);
		assertEquals(5, writtenBytes[0]);
		stream.close();
	}

	/**
	 * This method tests writing special characters by embedding them into a
	 * other data and allows choosing the conversion mode of the underlying
	 * output stream.
	 * 
	 * @param specialChars
	 *            the special characters as to be written into the stream
	 * @param expectedChars
	 *            the characters that are expected after reading from the
	 *            {@link FakeOutputStream#getWrittenBytes()} method.
	 * @param convert
	 *            if the used stream should convert characters
	 * @throws IOException
	 */
	private void testWriteSpecial(byte[] specialChars, byte[] expectedChars,
			boolean convert) throws IOException {
		FakeOutputStream stream = new FakeOutputStream(convert);
		stream.write(Utils.concatByteArrays(new byte[] { 'a' }, specialChars,
				new byte[] { 'b' }));

		byte[] writtenBytes = stream.getWrittenBytes();
		assertEquals(writtenBytes.length, expectedChars.length + 2);
		assertEquals('a', writtenBytes[0]);
		assertEquals('b', writtenBytes[writtenBytes.length - 1]);
		for (int i = 1; i < writtenBytes.length - 1; i++) {
			assertEquals(expectedChars[i - 1], writtenBytes[i]);
		}
		stream.close();
	}

	/**
	 * Positive test case: Write and expect CR
	 * 
	 * @throws IOException
	 */
	@Test
	public void testWriteCrNoConversion() throws IOException {
		testWriteSpecial(cr, cr, false);
	}

	/**
	 * Positive test case: Write and expect LF
	 * 
	 * @throws IOException
	 */
	@Test
	public void testWriteLfNoConversion() throws IOException {
		testWriteSpecial(lf, lf, false);
	}

	/**
	 * Positive test case: Write and expect CRLF
	 * 
	 * @throws IOException
	 */
	@Test
	public void testWriteCrLfNoConversion() throws IOException {
		testWriteSpecial(crlf, crlf, false);
	}

	/**
	 * Positive test case: Write CR and expect LF after conversion
	 * 
	 * @throws IOException
	 */
	@Test
	public void testWriteCrConversion() throws IOException {
		testWriteSpecial(cr, lf, true);
	}

	/**
	 * Positive test case: Write LF and expect LF after conversion
	 * 
	 * @throws IOException
	 */
	@Test
	public void testWriteLfConversion() throws IOException {
		testWriteSpecial(lf, lf, true);
	}

	/**
	 * Positive test case: Write CRLF and expect LF after conversion
	 * 
	 * @throws IOException
	 */
	@Test
	public void testWriteCrLfConversion() throws IOException {
		testWriteSpecial(crlf, lf, true);
	}

	/**
	 * Positive test case: Using the LFCR and expecting LFCR without conversion
	 * 
	 * @throws IOException
	 */
	@Test
	public void testWriteLfCrNoConversion() throws IOException {
		testWriteSpecial(Utils.concatByteArrays(lf, cr),
				Utils.concatByteArrays(lf, cr), false);
	}

	/**
	 * Positive test case: Using the LFCR and expecting two LF after conversion
	 * 
	 * @throws IOException
	 */
	@Test
	public void testWriteLfCrConversion() throws IOException {
		testWriteSpecial(Utils.concatByteArrays(lf, cr),
				Utils.concatByteArrays(lf, lf), true);
	}
}
