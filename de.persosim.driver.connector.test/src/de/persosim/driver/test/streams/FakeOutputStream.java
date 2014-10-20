package de.persosim.driver.test.streams;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class FakeOutputStream extends OutputStream {
	boolean ignoreNextIfNewLine = false;
	
	List<Byte> received = new ArrayList<>();

	private boolean convertLinefeeds;
	
	public FakeOutputStream(boolean convertLinefeeds) {
		this.convertLinefeeds = convertLinefeeds;
	}
	
	@Override
	public void write(int b) throws IOException {
		if (convertLinefeeds){
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
				received.add((byte)b);
			}	
		} else{
			received.add((byte)b);
		}
		
	}
	
	public byte [] getWrittenBytes(){
		byte [] result = new byte [received.size()];
		for (int i = 0; i < result.length; i++){
			result[i] = received.get(i);
		}
		return result;
	}

}
