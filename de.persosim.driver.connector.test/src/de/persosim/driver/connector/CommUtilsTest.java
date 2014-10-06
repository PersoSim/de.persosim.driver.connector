package de.persosim.driver.connector;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.junit.Before;
import org.junit.Test;

import de.persosim.driver.connector.CommUtils.HandshakeMode;
import de.persosim.driver.connector.exceptions.PcscNativeCommunicationException;
import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.test.ConnectorTest;
import de.persosim.simulator.utils.Utils;

public class CommUtilsTest extends ConnectorTest {

	@Mocked
	Socket dataSocket;

	@Before
	public void setUp() {
	}

	/**
	 * Positive test
	 * 
	 * @throws Exception
	 */
	@Test
	public void testExchangeApdu() throws Exception {

		final byte[] testData = new byte[] { 1, 2, 3, 4 };

		final LinkedList<Byte> buffer = new LinkedList<Byte>();

		final InputStream input = new InputStream() {
			@Override
			public int read() throws IOException {
				if (buffer.size() > 0) {
					return buffer.removeLast();
				}
				return -1;
			}
		};

		final OutputStream output = new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				buffer.addFirst((byte) b);
			}
		};

		new NonStrictExpectations() {
			{
				dataSocket.getInputStream();
				result = input;
				dataSocket.getOutputStream();
				result = output;
			}
		};

		assertArrayEquals(testData,
				CommUtils.exchangeApdu(dataSocket, testData));
		input.close();
		output.close();
	}

	/**
	 * This method simulates a handshake with a native driver by mocking the
	 * used socket. It checks the expected data written into the mocked socket
	 * by the
	 * {@link CommUtils#doHandshake(Socket, UnsignedInteger, HandshakeMode)}
	 * method by comparing it to the given expected messages. \r \r\n and \n are
	 * all regarded equal. The socket simulates a driver answering with the data
	 * given in the first parameter.
	 * 
	 * @param simulatedDriverAnswers
	 *            a string of messages divided by a new line
	 * @param expectedConnectorHandshakeMessages
	 *            a string of messages divided by a new line
	 * @param mode
	 * @param lun
	 * @param expectedLun
	 * @throws Exception
	 */
	private void testHandshakeWithConnector(String simulatedDriverAnswers,
			String expectedConnectorHandshakeMessages, HandshakeMode mode,
			UnsignedInteger lun, UnsignedInteger expectedLun) throws Exception {

		final byte[] answers = simulatedDriverAnswers.getBytes();
		final byte[] expected = expectedConnectorHandshakeMessages.getBytes();
		final StringBuilder received = new StringBuilder();

		final InputStream input = new InputStream() {
			int current = 0;

			@Override
			public int read() throws IOException {
				if (answers.length > current) {
					return answers[current++];
				}
				return -1;
			}
		};

		final OutputStream output = new OutputStream() {
			boolean ignoreNext = false;

			@Override
			public void write(int b) throws IOException {
				if (ignoreNext && (char) b == '\n') {
					return;
				}
				if ((char) b == '\r') {
					ignoreNext = true;
				} else {
					received.append((char)b);
				}
			}
		};

		new NonStrictExpectations() {
			{
				dataSocket.getInputStream();
				result = input;
				dataSocket.getOutputStream();
				result = output;
			}
		};
		
		UnsignedInteger receivedLun = CommUtils.doHandshake(dataSocket, lun, mode);
		assertArrayEquals(expected, received.toString().getBytes());
		assertEquals(expectedLun, receivedLun);
		
		input.close();
		output.close();
	}

	/**
	 * Positive test
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDoHandshake() throws Exception {

		String simulatedDriverAnswers = NativeDriverInterface.MESSAGE_IFD_HELLO
				.getAsHexString() + "|00000000\n";
		String expectedConnectorHandshakeMessages = NativeDriverInterface.MESSAGE_ICC_HELLO
				.getAsHexString()
				+ NativeDriverInterface.MESSAGE_DIVIDER
				+ "FFFFFFFF\n"
				+ NativeDriverInterface.MESSAGE_ICC_DONE.getAsHexString()
				+ "\n";

		testHandshakeWithConnector(simulatedDriverAnswers, expectedConnectorHandshakeMessages,
				HandshakeMode.OPEN, NativeDriverInterface.LUN_NOT_ASSIGNED,
				new UnsignedInteger(0));
	}

	/**
	 * Positive test for closing handshake
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDoHandshakeClosing() throws Exception {

		String simulatedDriverAnswers = NativeDriverInterface.MESSAGE_IFD_HELLO
				.getAsHexString() + "|00000000\n";
		String expectedConnectorHandshakeMessages = NativeDriverInterface.MESSAGE_ICC_HELLO
				.getAsHexString()
				+ NativeDriverInterface.MESSAGE_DIVIDER
				+ "00000000\n"
				+ NativeDriverInterface.MESSAGE_ICC_STOP.getAsHexString()
				+ "\n";

		testHandshakeWithConnector(simulatedDriverAnswers, expectedConnectorHandshakeMessages,
				HandshakeMode.CLOSE, new UnsignedInteger(0),
				new UnsignedInteger(0));
	}

	/**
	 * Negative test using an unknown message type
	 * 
	 * @throws Exception
	 */
	@Test(expected = PcscNativeCommunicationException.class)
	public void testDoHandshakeUnknownMessage() throws Exception {

		String simulatedDriverAnswers = "12345678|00000000\n";
		String expectedConnectorHandshakeMessages = NativeDriverInterface.MESSAGE_ICC_HELLO
				.getAsHexString()
				+ NativeDriverInterface.MESSAGE_DIVIDER
				+ "FFFFFFFF\n"
				+ NativeDriverInterface.MESSAGE_ICC_DONE.getAsHexString()
				+ "\n";

		testHandshakeWithConnector(simulatedDriverAnswers, expectedConnectorHandshakeMessages,
				HandshakeMode.OPEN, NativeDriverInterface.LUN_NOT_ASSIGNED, null);
	}

	/**
	 * Positive test using an known Lun
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDoHandshakeReuseLun() throws Exception {

		String simulatedDriverAnswers = NativeDriverInterface.MESSAGE_IFD_HELLO
				.getAsHexString() + "|00000004\n";
		String expectedConnectorHandshakeMessages = NativeDriverInterface.MESSAGE_ICC_HELLO
				.getAsHexString()
				+ NativeDriverInterface.MESSAGE_DIVIDER
				+ "00000004\n"
				+ NativeDriverInterface.MESSAGE_ICC_DONE.getAsHexString()
				+ "\n";

		testHandshakeWithConnector(simulatedDriverAnswers, expectedConnectorHandshakeMessages,
				HandshakeMode.OPEN, new UnsignedInteger(4),
				new UnsignedInteger(4));
	}

	/**
	 * Positive test
	 */
	@Test
	public void testGetNullTerminatedAsciiString() {
		String testData = "test";
		byte[] expected = Utils.concatByteArrays(
				testData.getBytes(StandardCharsets.US_ASCII), new byte[] { 0 });

		assertArrayEquals(expected,
				CommUtils.getNullTerminatedAsciiString(testData));
	}

	/**
	 * Positive test
	 */
	@Test
	public void testToUnsignedShortFlippedBytes() {
		short testData = 0x1234;
		byte[] expected = new byte[] { 0x34, 0x12 };
		assertArrayEquals(expected,
				CommUtils.toUnsignedShortFlippedBytes(testData));
	}

	/**
	 * Positive test using unsigned values
	 */
	@Test
	public void testToUnsignedShortFlippedBytesUnsigned() {
		short testData = (short) 0xF1FA;
		byte[] expected = new byte[] { (byte) 0xFA, (byte) 0xF1 };
		assertArrayEquals(expected,
				CommUtils.toUnsignedShortFlippedBytes(testData));
	}

	/**
	 * Positive test
	 */
	@Test
	public void testGetExpectedLength() {
		PcscCallData callData = new PcscCallData(
				"00000000|00000000|ABCDEF|00000003");

		assertEquals(3, CommUtils.getExpectedLength(callData, 1)
				.getAsSignedLong());
	}

	/**
	 * Negative test using missing parameter
	 */
	@Test
	public void testGetExpectedLengthMissing() {
		PcscCallData callData = new PcscCallData("00000000|00000000|ABCDEF");

		assertEquals(null, CommUtils.getExpectedLength(callData, 1));
	}
}
