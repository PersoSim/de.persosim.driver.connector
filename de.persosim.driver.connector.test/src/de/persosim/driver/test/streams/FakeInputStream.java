package de.persosim.driver.test.streams;

import java.io.IOException;
import java.io.InputStream;

public class FakeInputStream extends InputStream{
	private int current = 0;
	byte [] data;

	public FakeInputStream(byte [] data) {
		this.data = data;
	}
	
	@Override
	public int read() throws IOException {
		if (data.length > current) {
			return data[current++];
		}
		return -1;
	}
}
