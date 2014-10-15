package de.persosim.driver.test;

import java.io.IOException;


public class TestSocketSim {
	private Thread communicationThread;
	private TestSocketSimComm communication;
	private int port;
	private TestApduHandler handler;
	private boolean isRunning;
	
	public TestSocketSim(int port, TestApduHandler handler) {
		this.port = port;
		this.handler = handler;
	}
	
	public void start() throws IOException{
		communication = new TestSocketSimComm(port, handler);
		communicationThread = new Thread(communication);
		communicationThread.start();
		isRunning = true;
	}
	
	public void stop() throws IOException, InterruptedException{
		if (isRunning){
			communication.stop();
			communicationThread.join();
		}
	}
	
}
