package de.persosim.driver.test;

import static de.persosim.driver.connector.NativeDriverComm.*;
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

		CommUtils.writeLine(writer, MESSAGE_ICC_HELLO + "|LUN:-1");
		assertEquals(MESSAGE_IFD_HELLO + "|LUN:0", reader.readLine());
		CommUtils.writeLine(writer, MESSAGE_ICC_DONE);
		assertEquals(MESSAGE_IFD_DONE, reader.readLine());
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
		Thread sim = new Thread(new Runnable() {
			// this runnable mocks the simulator
			@Override
			public void run() {

				String data;
				try {
					System.out.println("Sim waiting for data");
					data = reader.readLine();
					assertEquals("0#0#54657374", data);
					System.out.println("Sim writing data");

					writer.write("Answer");
					writer.newLine();
					writer.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});

		CommUtils.doHandshake(dataSocket, 0, HandshakeMode.OPEN);

		sim.start();
		
		String response = nativeDriver.sendData(0, 0, "Test".getBytes());
		assertEquals("Answer", response);
		while (message == null) {
			Thread.yield();
		}
		assertEquals("Answer", message);
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
