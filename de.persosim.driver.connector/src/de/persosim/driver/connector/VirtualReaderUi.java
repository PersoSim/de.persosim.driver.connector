package de.persosim.driver.connector;

import java.io.IOException;

public interface VirtualReaderUi {
	public abstract byte[] getPin() throws IOException;
	public abstract void display(String ... lines);
}
