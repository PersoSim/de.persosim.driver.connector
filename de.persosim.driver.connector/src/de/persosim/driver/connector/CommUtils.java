package de.persosim.driver.connector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import de.persosim.driver.connector.exceptions.PcscNativeCommunicationException;
import de.persosim.simulator.utils.HexString;
import de.persosim.simulator.utils.Utils;

/**
 * This class contains helper methods to facilitate the communication with the
 * simulation and driver components.
 * 
 * @author mboonk
 *
 */
public class CommUtils {
	/**
	 * The handshake can be done in several modes, this enum is used to
	 * differentiate.
	 * 
	 * @author mboonk
	 *
	 */
	public enum HandshakeMode {
		/**
		 * Used to open a new connection
		 */
		OPEN, /**
		 * Used to close an existing connection
		 */
		CLOSE;
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

	/**
	 * This method performs a handshake against the
	 * {@link NativeDriverConnector}.
	 * 
	 * @param iccSocket
	 *            the socket to communicate over
	 * @param lun
	 *            the logical unit number
	 * @param mode
	 *            {@link HandshakeMode}
	 * @return the assigned lun for this connection
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static int doHandshake(Socket iccSocket, int lun, HandshakeMode mode)
			throws IOException, InterruptedException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				iccSocket.getOutputStream()));
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				iccSocket.getInputStream()));

		CommUtils.writeLine(writer, NativeDriverInterface.MESSAGE_ICC_HELLO
				+ NativeDriverInterface.MESSAGE_DIVIDER + "LUN:" + lun);
		String[] helloData = reader.readLine().split(
				Pattern.quote(NativeDriverInterface.MESSAGE_DIVIDER));

		if (!helloData[0].equals(NativeDriverInterface.MESSAGE_IFD_HELLO)) {
			throw new PcscNativeCommunicationException(
					"Unexpected message type");
		}

		try {
			int responseLun = Integer.parseInt(helloData[1].split(":")[1]);

			if (mode == HandshakeMode.OPEN) {
				CommUtils.writeLine(writer,
						NativeDriverInterface.MESSAGE_ICC_DONE);
			} else {
				CommUtils.writeLine(writer,
						NativeDriverInterface.MESSAGE_ICC_STOP);
			}
			helloData = reader.readLine().split(
					Pattern.quote(NativeDriverInterface.MESSAGE_DIVIDER));

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

	/**
	 * This method writes a line using the given {@link BufferedWriter} and
	 * flushes it.
	 * 
	 * @param writer
	 * @param message
	 *            the message to send (it will be concatenated with a new line
	 *            separator)
	 * @throws IOException
	 */
	public static void writeLine(BufferedWriter writer, String message)
			throws IOException {
		writer.write(message);
		writer.newLine();
		writer.flush();
	}
	
	public static byte [] getNullTerminatedAsciiString(String string){
		byte [] bytes = string.getBytes(StandardCharsets.US_ASCII);
		return Utils.concatByteArrays(bytes, new byte [1]);
	}
}
