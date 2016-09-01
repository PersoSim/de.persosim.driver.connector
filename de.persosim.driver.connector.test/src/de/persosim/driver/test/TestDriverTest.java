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
import de.persosim.driver.connector.UnsignedInteger;

public class TestDriverTest extends ConnectorTest implements DriverEventListener {
	private String message;
	private TestDriver nativeDriver;

	@Before
	public void setUp() throws IOException {
		nativeDriver = new TestDriver();
		nativeDriver.addListener(this);
		nativeDriver.start(getTestDriverPort());
		message = null;
	}

	@After
	public void tearDown() throws InterruptedException {
		nativeDriver.stop();
	}

	@Test
	public void testHandshake() throws UnknownHostException, IOException,
			InterruptedException {
		Socket iccSocket = new Socket(getTestDriverHost(),
				getTestDriverPort());
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				iccSocket.getOutputStream()));
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				iccSocket.getInputStream()));

		CommUtils.writeLine(writer, NativeDriverInterface.MESSAGE_ICC_HELLO.getAsHexString() + "|" + new UnsignedInteger(-1).getAsHexString());
		assertEquals(NativeDriverInterface.MESSAGE_IFD_HELLO.getAsHexString() + "|" + new UnsignedInteger(0).getAsHexString(), reader.readLine());
		CommUtils.writeLine(writer, NativeDriverInterface.MESSAGE_ICC_DONE.getAsHexString());
		iccSocket.close();
	}

	@Test
	public void testSendData() throws IOException, InterruptedException {
		Socket dataSocket = new Socket(getTestDriverHost(),
				getTestDriverPort());
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
					assertEquals("00000000" + NativeDriverInterface.MESSAGE_DIVIDER + "00000000" + NativeDriverInterface.MESSAGE_DIVIDER + "54657374", data);
					System.out.println("Connector mock writing data");
					String response = "00112233"; 
					writer.write(response);
					writer.newLine();
					writer.flush();

					System.out.println("Connector mock sent data: " + response);
				} catch (IOException e) {
					// only log as we are in test code here
					e.printStackTrace();
				}

			}
		});

		UnsignedInteger lun = CommUtils.doHandshake(dataSocket, HandshakeMode.OPEN);

		connector.start();
		
		String response = nativeDriver.sendData(lun, new UnsignedInteger(0), "Test".getBytes());
		
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
