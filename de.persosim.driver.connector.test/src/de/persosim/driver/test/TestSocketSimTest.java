package de.persosim.driver.test;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import mockit.Expectations;
import mockit.Mocked;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestSocketSimTest extends ConnectorTest {
	TestSocketSim sim;

	@Mocked
	TestApduHandler handler;
	
	@Before
	public void setUp() throws IOException{
		sim = new TestSocketSim(getSimulatorDriverPort());
		sim.start();
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
		sim.setHandler(handler);
		new Expectations() {
			{
				handler.processCommand(withEqual("55667788"));
				result = "11223344";
			}
		};
		
		Socket socket = new Socket(getSimulatorDriverHost(), getSimulatorDriverPort());
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
