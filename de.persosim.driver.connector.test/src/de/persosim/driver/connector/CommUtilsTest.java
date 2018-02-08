package de.persosim.driver.connector;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.junit.Before;
import org.junit.Test;

import de.persosim.driver.connector.CommUtils.HandshakeMode;
import de.persosim.driver.connector.exceptions.PcscNativeCommunicationException;
import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.test.ConnectorTest;
import de.persosim.driver.test.streams.FakeInputStream;
import de.persosim.driver.test.streams.FakeOutputStream;
import de.persosim.simulator.utils.HexString;
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
		String expectedResponse = "00010203";
		byte[] expectedResponseBytes = HexString.toByteArray(expectedResponse);

		final InputStream input = new FakeInputStream(expectedResponse + "\n");

		final FakeOutputStream output = new FakeOutputStream(true);

		new NonStrictExpectations() {
			{
				dataSocket.getInputStream();
				result = input;
				dataSocket.getOutputStream();
				result = output;
			}
		};

		byte[] cmdApdu = HexString.toByteArray("00A4000000");
		assertArrayEquals(expectedResponseBytes,
				CommUtils.exchangeApdu(dataSocket, cmdApdu));
		
		byte [] expectedSocketMsg = Utils.appendBytes(HexString.encode(cmdApdu).getBytes(), (byte)'\n');
		assertEquals(new String(expectedSocketMsg), new String(output.getWrittenBytes()));
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

		final InputStream input = new FakeInputStream(answers);

		final FakeOutputStream output = new FakeOutputStream(true);

		new NonStrictExpectations() {
			{
				dataSocket.getInputStream();
				result = input;
				dataSocket.getOutputStream();
				result = output;
			}
		};
		
		UnsignedInteger receivedLun = CommUtils.doHandshake(dataSocket, lun, mode);
		assertArrayEquals(expected, output.getWrittenBytes());
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

		String simulatedDriverAnswers = IfdInterface.MESSAGE_IFD_HELLO
				.getAsHexString() + "|00000000\n";
		String expectedConnectorHandshakeMessages = IfdInterface.MESSAGE_ICC_HELLO
				.getAsHexString()
				+ IfdInterface.MESSAGE_DIVIDER
				+ "FFFFFFFF\n"
				+ IfdInterface.MESSAGE_ICC_DONE.getAsHexString()
				+ "\n";

		testHandshakeWithConnector(simulatedDriverAnswers, expectedConnectorHandshakeMessages,
				HandshakeMode.OPEN, IfdInterface.LUN_NOT_ASSIGNED,
				new UnsignedInteger(0));
	}

	/**
	 * Positive test for closing handshake
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDoHandshakeClosing() throws Exception {

		String simulatedDriverAnswers = IfdInterface.MESSAGE_IFD_HELLO
				.getAsHexString() + "|00000000\n";
		String expectedConnectorHandshakeMessages = IfdInterface.MESSAGE_ICC_HELLO
				.getAsHexString()
				+ IfdInterface.MESSAGE_DIVIDER
				+ "00000000\n"
				+ IfdInterface.MESSAGE_ICC_STOP.getAsHexString()
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
		String expectedConnectorHandshakeMessages = IfdInterface.MESSAGE_ICC_HELLO
				.getAsHexString()
				+ IfdInterface.MESSAGE_DIVIDER
				+ "FFFFFFFF\n"
				+ IfdInterface.MESSAGE_ICC_DONE.getAsHexString()
				+ "\n";

		testHandshakeWithConnector(simulatedDriverAnswers, expectedConnectorHandshakeMessages,
				HandshakeMode.OPEN, IfdInterface.LUN_NOT_ASSIGNED, null);
	}

	/**
	 * Positive test using an known Lun
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDoHandshakeReuseLun() throws Exception {

		String simulatedDriverAnswers = IfdInterface.MESSAGE_IFD_HELLO
				.getAsHexString() + "|00000004\n";
		String expectedConnectorHandshakeMessages = IfdInterface.MESSAGE_ICC_HELLO
				.getAsHexString()
				+ IfdInterface.MESSAGE_DIVIDER
				+ "00000004\n"
				+ IfdInterface.MESSAGE_ICC_DONE.getAsHexString()
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
	
	@Test
	public void testChangeByteOrder() {
		assertArrayEquals(new byte [] {1,2,3,4}, CommUtils.changeByteOrder(new byte [] {4,3,2,1}));
	}
}
