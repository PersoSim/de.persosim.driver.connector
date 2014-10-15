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
		sim = new TestSocketSim(SIMULATOR_PORT, handler);
		sim.start();
	}
	
	@After
	public void teardown() throws IOException, InterruptedException{
		sim.stop();
	}
	
	@Test
	public void testHandleApdu() throws UnknownHostException, IOException {

		new Expectations() {
			{
				handler.processCommand(withEqual("TESTSTRING"));
				result = "TESTRESULT";
			}
		};
		
		Socket socket = new Socket(SIMULATOR_HOST, SIMULATOR_PORT);
		BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		
		writer.write("TESTSTRING");
		writer.newLine();
		writer.flush();
		
		String result = reader.readLine();
		
		assertEquals("TESTRESULT", result);
		socket.close();
		
	}
}
