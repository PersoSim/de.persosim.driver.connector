package de.persosim.driver.connector;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Collection;
import java.util.HashSet;

import mockit.Expectations;
import mockit.Mocked;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.connector.pcsc.PcscListener;
import de.persosim.driver.test.TestDriver;

public class NativeDriverCommTest {
	
	private NativeDriverComm nativeComm;
	@Mocked
	private PcscListener mockedListener;
	private TestDriver driver;
	private Collection<PcscListener> listeners;

	@Before
	public void setUp() throws Exception{
		driver = new TestDriver();
		driver.start();
		listeners = new HashSet<>();
		nativeComm = new NativeDriverComm("localhost", TestDriver.PORT_NUMBER_DEFAULT, listeners);
	}
	
	@After
	public void tearDown() throws Exception {
		nativeComm.interrupt();
		driver.stop();
	}

	@Test(timeout=2000)
	public void testThread() throws IOException, InterruptedException{
//		prepare the mock

		listeners.add(mockedListener);
		new Expectations() {{
			mockedListener.processPcscCall((PcscCallData) any);
			result = null;
		}};
		
		nativeComm.start();
		
		Thread.sleep(100);
		
		Socket dataSocket = new Socket("localhost", TestDriver.PORT_NUMBER_DEFAULT);
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(dataSocket.getOutputStream()));
		writer.write("Test");
		writer.newLine();
		writer.flush();
		Thread.sleep(10);
		dataSocket.close();
		
	}
}
