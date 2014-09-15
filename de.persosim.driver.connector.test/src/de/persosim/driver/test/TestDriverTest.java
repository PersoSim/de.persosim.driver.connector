package de.persosim.driver.test;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.persosim.driver.connector.CommUtils;
import de.persosim.driver.connector.CommUtils.HandshakeMode;
import de.persosim.driver.connector.NativeDriverInterface;
import de.persosim.simulator.utils.HexString;

public class TestDriverTest implements DriverEventListener {
	private String message;
	private TestDriver nativeDriver;

	@Before
	public void setUp() throws IOException {
		nativeDriver = new TestDriver();
		nativeDriver.addListener(this);
		nativeDriver.start();
		message = null;
	}

	@After
	public void tearDown() throws InterruptedException {
		nativeDriver.stop();
	}

	@Test
	public void testHandshake() throws UnknownHostException, IOException,
			InterruptedException {
		Socket iccSocket = new Socket("localhost",
				TestDriver.PORT_NUMBER_DEFAULT);
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				iccSocket.getOutputStream()));
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				iccSocket.getInputStream()));

		CommUtils.writeLine(writer, HexString.encode(NativeDriverInterface.MESSAGE_ICC_HELLO) + "|" + HexString.encode((byte) -1));
		assertEquals(HexString.encode(NativeDriverInterface.MESSAGE_IFD_HELLO) + "|" + HexString.encode((byte)0), reader.readLine());
		CommUtils.writeLine(writer, HexString.encode(NativeDriverInterface.MESSAGE_ICC_DONE));
		assertEquals(HexString.encode(NativeDriverInterface.MESSAGE_IFD_DONE), reader.readLine());
		iccSocket.close();
	}

	@Test
	public void testSendData() throws IOException, InterruptedException {
		final Socket dataSocket = new Socket("localhost",
				TestDriver.PORT_NUMBER_DEFAULT);
		final BufferedReader reader = new BufferedReader(new InputStreamReader(
				dataSocket.getInputStream()));
		final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				dataSocket.getOutputStream()));
		Thread connector = new Thread(new Runnable() {
			// this runnable mocks the connector
			@Override
			public void run() {

				String data;
				try {
					System.out.println("Connector mock waiting for data");
					data = reader.readLine();
					System.out.println("Connector mock received data: " + data);
					assertEquals("00" + NativeDriverInterface.MESSAGE_DIVIDER + "00" + NativeDriverInterface.MESSAGE_DIVIDER + "54657374", data);
					System.out.println("Connector mock writing data");
					String response = "00112233"; 
					writer.write(response);
					writer.newLine();
					writer.flush();

					System.out.println("Connector mock sent data: " + response);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});

		byte lun = CommUtils.doHandshake(dataSocket, (byte) -1, HandshakeMode.OPEN);

		connector.start();
		
		String response = nativeDriver.sendData(lun, (byte)0, "Test".getBytes());
		
		assertEquals("00112233", response);
		while (message == null) {
			Thread.yield();
		}
		assertEquals("00112233", message);
		dataSocket.close();
	}

	@Override
	public void messageReceivedCallback(String message) {
		this.message = message;
	}

	@Override
	public void handshakeMessageReceivedCallback(String handshakeMessage) {
		//implement
	}
}
