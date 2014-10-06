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
import de.persosim.driver.connector.pcsc.PcscCallData;
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
	 * @throws IOException
	 */
	public static byte[] exchangeApdu(Socket socket, byte[] commandApdu)
			throws IOException {
		BufferedReader bufferedIn = new BufferedReader(new InputStreamReader(
				socket.getInputStream()));
		PrintWriter printOut;
		printOut = new PrintWriter(socket.getOutputStream());
		printOut.println(HexString.encode(commandApdu));
		printOut.flush();
		return HexString.toByteArray(bufferedIn.readLine());
	}

	/**
	 * This method performs a handshake against the
	 * {@link NativeDriverConnector} if no lun is known. This is the case for
	 * the initial connection initiation.
	 * 
	 * @param iccSocket
	 *            the socket to communicate over
	 * @param mode
	 *            {@link HandshakeMode}
	 * @return the assigned lun for this connection
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static UnsignedInteger doHandshake(Socket iccSocket,
			HandshakeMode mode) throws IOException, InterruptedException {
		return doHandshake(iccSocket, NativeDriverInterface.LUN_NOT_ASSIGNED, mode);
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
	public static UnsignedInteger doHandshake(Socket iccSocket,
			UnsignedInteger lun, HandshakeMode mode) throws IOException,
			InterruptedException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				iccSocket.getOutputStream()));
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				iccSocket.getInputStream()));

		CommUtils.writeLine(
				writer,
				NativeDriverInterface.MESSAGE_ICC_HELLO.getAsHexString()
						+ NativeDriverInterface.MESSAGE_DIVIDER
						+ lun.getAsHexString());
		String[] helloData = reader.readLine().split(
				Pattern.quote(NativeDriverInterface.MESSAGE_DIVIDER));

		if (!(new UnsignedInteger(HexString.toByteArray(helloData[0])))
				.equals(NativeDriverInterface.MESSAGE_IFD_HELLO)) {
			throw new PcscNativeCommunicationException(
					"Unexpected message type");
		}

		try {
			UnsignedInteger responseLun = UnsignedInteger.parseUnsignedInteger(
					helloData[1], 16);

			if (mode == HandshakeMode.OPEN) {
				CommUtils
						.writeLine(writer,
								NativeDriverInterface.MESSAGE_ICC_DONE
										.getAsHexString());
			} else if (mode == HandshakeMode.CLOSE) {
				CommUtils
						.writeLine(writer,
								NativeDriverInterface.MESSAGE_ICC_STOP
										.getAsHexString());
			} else {
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

	public static byte[] getNullTerminatedAsciiString(String string) {
		byte[] bytes = string.getBytes(StandardCharsets.US_ASCII);
		return Utils.concatByteArrays(bytes, new byte[1]);
	}

	public static byte[] toUnsignedShortFlippedBytes(short value) {
		return new byte[] { (byte) (0xFF & value), (byte) (0xFF & value >>> 8) };
	}

	/**
	 * The expected length is stored as a parameter in the data object.
	 * 
	 * @param data
	 * @param expectedLengthParameterIndex
	 * @return the parameter with the given index as {@link UnsignedInteger} or
	 *         null if the given index is out of range
	 */
	public static UnsignedInteger getExpectedLength(PcscCallData data,
			int expectedLengthParameterIndex) {
		if (expectedLengthParameterIndex < data.getParameters().size() && expectedLengthParameterIndex > 0) {
			return new UnsignedInteger(data.getParameters().get(
					expectedLengthParameterIndex));
		}
		return null;
	}
}
