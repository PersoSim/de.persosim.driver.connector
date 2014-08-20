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

public class NativeDriverTest implements DriverEventListener {
	private String message;
	private String event;
	private NativeDriver nativeDriver;
	
	@Before
	public void setUp() throws IOException{
		nativeDriver = new NativeDriver();
		nativeDriver.addListener(this);
		message = null;
		event = null;
	}
	
	@After
	public void tearDown() throws InterruptedException{
		nativeDriver.stopDriver();
	}
	
	@Test
	public void testSendData() throws IOException, InterruptedException {
		final Socket dataSocket = new Socket("localhost", NativeDriver.PORT_NUMBER_DATA_DEFAULT);
		
		Thread sim = new Thread(new Runnable() {
			//this runnable mocks the simulator
			@Override
			public void run() {

				String data;
				try {
					System.out.println("Sim waiting for data");
					BufferedReader reader = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()));
					while(!reader.ready())
			        {
			            Thread.yield();
			        }
					data = reader.readLine();
					assertEquals("Test", data);
					System.out.println("Sim writing data");
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(dataSocket.getOutputStream()));
					writer.write("Answer");
					writer.newLine();
					writer.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		});
		sim.start();
		while(!sim.isAlive()){
			Thread.yield();
		}
		String response = nativeDriver.sendData("Test");
		assertEquals("Answer", response);
		while(message == null){
			Thread.yield();
		}
		assertEquals("Answer", message);
		dataSocket.close();
	}
	
	@Test(timeout=2000)
	public void testEventReceivedCallback() throws UnknownHostException, IOException, InterruptedException{
		Socket eventSocket = new Socket("localhost", NativeDriver.PORT_NUMBER_EVENTS_DEFAULT);
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(eventSocket.getOutputStream()));
		writer.write("Event");
		writer.newLine();
		writer.flush();	
		while(event == null){
			Thread.yield();
		}
		assertEquals(event, "Event");
		eventSocket.close();
	}
	
	@Override
	public void messageReceivedCallback(String message) {
		this.message = message;
	}

	@Override
	public void eventReceivedCallback(String event) {
		this.event = event;	
	}
}
