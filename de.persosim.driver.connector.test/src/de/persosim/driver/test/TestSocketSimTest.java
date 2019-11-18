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

public class TestSocketSimTest extends ConnectorTest {
	TestSocketSim sim;

	@Before
	public void setUp() throws IOException{
		sim = new TestSocketSim();
		sim.start(getSimulatorServerSocket());
	}
	
	@After
	public void teardown() throws IOException, InterruptedException{
		sim.stop();
	}
	
	/**
	 * Positive test using 2 arbitrary strings.
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	@Test
	public void testHandleApdu() throws UnknownHostException, IOException {
		sim.setHandler(new TestApduHandler() {

			@Override
			public String processCommand(String apduLine) {
				assertEquals("55667788", apduLine);
				return "11223344";
			}
			
		});
		
		Socket socket = getSimulatorSocket();
		BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		
		writer.write("55667788");
		writer.newLine();
		writer.flush();
		
		String result = reader.readLine();
		
		assertEquals("11223344", result);
		socket.close();
		
	}
}
