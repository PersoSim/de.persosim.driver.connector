package de.persosim.driver.test;

import java.io.IOException;


public class TestSocketSim {
	private Thread communicationThread;
	private TestSocketSimComm communication;
	private int port;
	private boolean isRunning;
	
	public TestSocketSim(int port) {
		this.port = port;
	}
	
	public void start() throws IOException{
		communication = new TestSocketSimComm(port);
		communicationThread = new Thread(communication);
		communicationThread.start();
		while (!communication.isRunning()){
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		isRunning = true;
	}
	
	public void setHandler(TestApduHandler handler){
		if (communication != null){
			communication.setHandler(handler);
		} else {
			throw new NullPointerException("The communication thread is not running, therefore no handler can be set");
		}
	}
	
	public void stop() throws IOException, InterruptedException{
		if (isRunning){
			communication.stop();
			communicationThread.join();
		}
	}
	
}
