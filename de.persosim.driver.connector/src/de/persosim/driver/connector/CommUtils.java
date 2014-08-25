package de.persosim.driver.connector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import de.persosim.driver.connector.exceptions.PcscNativeCommunicationException;

public class CommUtils {
	public enum HandshakeMode {
		OPEN, CLOSE;
	}
	public static int doHandshake(Socket iccSocket, int lun, HandshakeMode mode) throws IOException,
			InterruptedException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				iccSocket.getOutputStream()));
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				iccSocket.getInputStream()));

		CommUtils.writeLine(writer, NativeDriverComm.MESSAGE_ICC_HELLO + "|LUN:" + lun);
		String [] helloData = reader.readLine().split("\\|");
		
		if (!helloData[0].equals(NativeDriverComm.MESSAGE_IFD_HELLO)){
			throw new PcscNativeCommunicationException("Unexpected message type");
		}
		
		try{
			int responseLun = Integer.parseInt(helloData[1].split(":")[1]);
			
			if (mode == HandshakeMode.OPEN){
				CommUtils.writeLine(writer, NativeDriverComm.MESSAGE_ICC_DONE);
			} else {
				CommUtils.writeLine(writer, NativeDriverComm.MESSAGE_ICC_STOP);
			}
			helloData = reader.readLine().split("\\|");
			
			if (!helloData[0].equals(NativeDriverComm.MESSAGE_IFD_DONE)){
				throw new PcscNativeCommunicationException("Unexpected message type");
			}
			
			return responseLun;
		} catch (NumberFormatException e){
			throw new PcscNativeCommunicationException("Unexpected message content");
		}
	}

	public static void writeLine(BufferedWriter writer, String message)
			throws IOException {
		writer.write(message);
		writer.newLine();
		writer.flush();
	}
}
