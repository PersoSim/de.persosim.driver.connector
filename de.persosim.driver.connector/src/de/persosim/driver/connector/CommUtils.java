package de.persosim.driver.connector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class CommUtils {
	public static int doHandshake(Socket iccSocket) throws IOException,
			InterruptedException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				iccSocket.getOutputStream()));
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				iccSocket.getInputStream()));

		CommUtils.writeLine(writer, NativeDriverComm.MESSAGE_ICC_HELLO + "|LUN:-1");
		String helloData = reader.readLine();
		
		CommUtils.writeLine(writer, NativeDriverComm.MESSAGE_ICC_DONE);
		reader.readLine();
		
		return Integer.parseInt(helloData.split("\\|")[1].split(":")[1]);
	}

	public static void writeLine(BufferedWriter writer, String message)
			throws IOException {
		writer.write(message);
		writer.newLine();
		writer.flush();
	}
}
