package de.persosim.driver.connector;

import java.io.IOException;

/**
 * This interface describes the user interfaces of the virtual reader, e.g. a
 * pin pad and a display.
 * 
 * @author mboonk
 *
 */
public interface VirtualReaderUi {
	/**
	 * Prompt the user to enter a PIN
	 * @return the PIN data
	 * @throws IOException
	 */
	public abstract byte[] getPin() throws IOException;

	/**
	 * Show a string based display to the user.
	 * @param lines the lines to display a multiple {@link String} objects
	 */
	public abstract void display(String... lines);
}
