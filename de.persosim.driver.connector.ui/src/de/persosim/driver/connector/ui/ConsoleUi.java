package de.persosim.driver.connector.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import de.persosim.driver.connector.CommUtils;
import de.persosim.driver.connector.VirtualReaderUi;
import de.persosim.driver.connector.pcsc.PcscConstants;
import de.persosim.simulator.utils.Utils;

/**
 * This class represents a console user interface for pin entry and string
 * display.
 * 
 * @author mboonk
 *
 */
public class ConsoleUi implements VirtualReaderUi {

	BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
	
	@Override
	public byte[] getPin() throws IOException {
		System.out.println("(enter empty password to cancel)");
		String line = reader.readLine();
		byte [] result = new byte [line.length()];
		for (int i = 0; i < line.length(); i++){
			result[i] = (byte) line.charAt(i);
		}
		if (result.length == 0){
			return null;
		}
		return result;
	}

	@Override
	public void display(String... lines) {
		int maxChars = 0;
		StringBuilder builder = new StringBuilder();
		
		for (String line : lines){
			if (maxChars < line.length()){
				maxChars = line.length();
			}
		}
		
		for (String line : lines){
			builder.append("|");
			builder.append(line);
			for (int i = line.length(); i < maxChars + 2 ; i++){
				builder.append(" ");
			}
			builder.append("|\n");
		}
		System.out.println("Virtual Reader Display\n");
		printCrossedLine(maxChars + 2);
		System.out.print("\n" + builder.toString());
		printCrossedLine(maxChars + 2);
		System.out.println();
	}
	
	/**
	 * This methods prints a line using '+' for the end points and uses '-' for
	 * the line itself.
	 * 
	 * @param length
	 *            the length of the line in characters between the end points
	 */
	private void printCrossedLine(int length) {
		System.out.print("+");
		for (int i = 0; i < length; i++) {
			System.out.print("-");
		}
		System.out.print("+");
	}

	@Override
	public byte[] getDeviceDescriptors() {
		byte [] result = CommUtils.getNullTerminatedAsciiString("PersoSim Console PinPad");
		result = Utils.concatByteArrays(result, PcscConstants.DEVICE_TYPE_FUNCTIONAL.getAsByteArray());
		result = Utils.concatByteArrays(result, CommUtils.getNullTerminatedAsciiString("PersoSim Console Display"));
		return Utils.concatByteArrays(result, PcscConstants.DEVICE_TYPE_FUNCTIONAL.getAsByteArray());
	}

}
