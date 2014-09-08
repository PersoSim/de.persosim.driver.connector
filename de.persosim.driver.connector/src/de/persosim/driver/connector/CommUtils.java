package de.persosim.driver.connector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.regex.Pattern;

import de.persosim.driver.connector.exceptions.PcscNativeCommunicationException;
import de.persosim.simulator.utils.HexString;

public class CommUtils {
	public enum HandshakeMode {
		OPEN, CLOSE;
	}

	/**
	 * @param socket
	 *            the socket to be used for sending, it needs to be open and
	 *            connected
	 * @param commandApdu
	 *            the data to transmit
	 * @return the response received on the socket or null in case of errors
	 */
	public static byte[] exchangeApdu(Socket socket, byte[] commandApdu) {
		try {
			BufferedReader bufferedIn = new BufferedReader(
					new InputStreamReader(socket.getInputStream()));
			PrintWriter printOut;
			printOut = new PrintWriter(socket.getOutputStream());
			printOut.println(HexString.encode(commandApdu));
			printOut.flush();
			return HexString.toByteArray(bufferedIn.readLine());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static int doHandshake(Socket iccSocket, int lun, HandshakeMode mode)
			throws IOException, InterruptedException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				iccSocket.getOutputStream()));
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				iccSocket.getInputStream()));

		CommUtils.writeLine(writer, NativeDriverInterface.MESSAGE_ICC_HELLO
				+ "|LUN:" + lun);
		String[] helloData = reader.readLine().split(Pattern.quote(NativeDriverInterface.MESSAGE_DIVIDER));

		if (!helloData[0].equals(NativeDriverInterface.MESSAGE_IFD_HELLO)) {
			throw new PcscNativeCommunicationException(
					"Unexpected message type");
		}

		try {
			int responseLun = Integer.parseInt(helloData[1].split(":")[1]);

			if (mode == HandshakeMode.OPEN) {
				CommUtils.writeLine(writer, NativeDriverInterface.MESSAGE_ICC_DONE);
			} else {
				CommUtils.writeLine(writer, NativeDriverInterface.MESSAGE_ICC_STOP);
			}
			helloData = reader.readLine().split(Pattern.quote(NativeDriverInterface.MESSAGE_DIVIDER));

			if (!helloData[0].equals(NativeDriverInterface.MESSAGE_IFD_DONE)) {
				throw new PcscNativeCommunicationException(
						"Unexpected message type");
			}

			return responseLun;
		} catch (NumberFormatException e) {
			throw new PcscNativeCommunicationException(
					"Unexpected message content");
		}
	}

	public static void writeLine(BufferedWriter writer, String message)
			throws IOException {
		writer.write(message);
		writer.newLine();
		writer.flush();
	}
}
