package de.persosim.driver.test.streams;

import java.io.IOException;
import java.io.InputStream;

/**
 * This fake stream can be used for testing. It supplies the reader with data
 * given in the constructor.
 * 
 * @author mboonk
 *
 */
public class FakeInputStream extends InputStream {
	private int current = 0;
	byte[] data;

	/**
	 * Creates a fake stream reading from a byte array.
	 * 
	 * @param data
	 */
	public FakeInputStream(byte[] data) {
		this.data = data;
	}

	/**
	 * Creates a fake stream reading from a String converted to bytes.
	 * 
	 * @param string
	 */
	public FakeInputStream(String string) {
		this(string.getBytes());
	}

	@Override
	public int read() throws IOException {
		if (data.length > current) {
			return data[current++];
		}
		return -1;
	}
}
