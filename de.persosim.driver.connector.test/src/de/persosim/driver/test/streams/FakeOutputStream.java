package de.persosim.driver.test.streams;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is an {@link OutputStream} that can be used for testing. It
 * provides the possibility to get all bytes that were written to this stream.
 * 
 * @author mboonk
 *
 */
public class FakeOutputStream extends OutputStream {
	boolean ignoreNextIfNewLine = false;

	List<Byte> received = new ArrayList<>();

	private boolean convertLinefeeds;

	/**
	 * Creates a fake stream. This stream can convert the the written line feeds
	 * to facilitate testing using different platforms. If the line feeds are
	 * converted, all \r, \n and combinations are changed into \n.
	 * 
	 * @param convertLinefeeds
	 */
	public FakeOutputStream(boolean convertLinefeeds) {
		this.convertLinefeeds = convertLinefeeds;
	}

	@Override
	public void write(int b) throws IOException {
		if (convertLinefeeds) {
			if (ignoreNextIfNewLine) {
				ignoreNextIfNewLine = false;
				if ((char) b == '\n') {
					return;
				}
			}
			if ((char) b == '\r') {
				ignoreNextIfNewLine = true;
				received.add((byte) '\n');
			} else {
				received.add((byte) b);
			}
		} else {
			received.add((byte) b);
		}

	}

	/**
	 * @see #FakeOutputStream(boolean);
	 * @return all bytes that were written to this fake stream, potentially with
	 *         converted line feeds
	 */
	public byte[] getWrittenBytes() {
		byte[] result = new byte[received.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = received.get(i);
		}
		return result;
	}

}
