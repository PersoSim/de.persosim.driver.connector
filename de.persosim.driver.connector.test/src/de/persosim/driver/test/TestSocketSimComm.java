package de.persosim.driver.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class TestSocketSimComm implements Runnable {

	private ServerSocket serverSocket;
	private TestApduHandler handler;
	private boolean isActive;
	private boolean isRunning = false;
	private Socket clientSocket;
	
	public TestSocketSimComm(int port) throws IOException{
		serverSocket = new ServerSocket(port);
		System.out.println("Starting TestSocketSim on port: " + port);
	}
	
	@Override
	public void run() {
		clientSocket = null;
		isActive = true;
		isRunning = true;
		try {
			clientSocket = serverSocket.accept();
			System.out.println("New TestSocketSim connection on port: " + clientSocket.getLocalPort());

			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			PrintStream out = new PrintStream(clientSocket.getOutputStream());

			do {
				String apduLine = in.readLine();
				if (apduLine == null){
					break;
				}
				System.out.println("TestSocketSim received apdu: " + apduLine);
				if (handler != null){
					String result = handler.processCommand(apduLine);
					System.out.println("TestSocketSim sent response: " + result);
					out.println(result);
				}
				out.flush();

			} while (isActive);
			clientSocket.close();
		} catch (SocketException e){
			// expected behavior if closed
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

	public void setHandler(TestApduHandler handler) {
		this.handler = handler;
	}
	
	
}
