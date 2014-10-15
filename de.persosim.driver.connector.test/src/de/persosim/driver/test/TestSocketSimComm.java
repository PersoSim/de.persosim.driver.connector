package de.persosim.driver.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TestSocketSimComm implements Runnable {

	private ServerSocket serverSocket;
	private TestApduHandler handler;
	private boolean isActive;
	private boolean isRunning = false;
	private Socket clientSocket;
	
	public TestSocketSimComm(int port, TestApduHandler handler) throws IOException{
		serverSocket = new ServerSocket(port);
		this.handler = handler;
	}
	
	@Override
	public void run() {
		clientSocket = null;
		isActive = true;
		isRunning = true;
		try {
			clientSocket = serverSocket.accept();

			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			PrintStream out = new PrintStream(clientSocket.getOutputStream());

			do {
				String apduLine = in.readLine();
				out.println(handler.processCommand(apduLine));
				out.flush();

			} while (isActive);
		}catch (IOException e){
			e.printStackTrace();
		}
	}

	public void stop() throws IOException{
		isActive = false;
		if (clientSocket != null){
			clientSocket.close();
		}
		serverSocket.close();
		isRunning = false;
	}

	public boolean isRunning() {
		return isRunning;
	}
	
	
}
