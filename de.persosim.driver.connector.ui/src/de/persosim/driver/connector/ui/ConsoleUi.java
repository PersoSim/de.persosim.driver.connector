package de.persosim.driver.connector.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import de.persosim.driver.connector.VirtualReaderUi;

public class ConsoleUi implements VirtualReaderUi {

	BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
	
	@Override
	public byte[] getPin() throws IOException {
		String line = reader.readLine();
		byte [] result = new byte [line.length()];
		for (int i = 0; i < line.length(); i++){
			result[i] = (byte) Integer.parseInt(line.charAt(i) + "");
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
			builder.append("|");
			builder.append(line);
			builder.append("|\n");
		}
		System.out.println("Virtual Reader Display\n");
		printCrossedLine(maxChars + 2);
		System.out.print(builder.toString());
		printCrossedLine(maxChars + 2);
	}
	
	private void printCrossedLine(int length){
		System.out.print("+");
		for (int i = 0; i < length; i++){
			System.out.print("-");
		}
		System.out.print("+");
	}

}
