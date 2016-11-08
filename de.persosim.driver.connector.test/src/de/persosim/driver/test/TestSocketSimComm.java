package de.persosim.driver.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * This class is the communication part of the {@link TestSocketSim}. It
 * controls the server socket and the communication with the client. If no
 * {@link TestApduHandler} is set, the answer to all requests is the empty
 * string.
 * 
 * @author mboonk
 *
 */
public class TestSocketSimComm implements Runnable {

	private ServerSocket serverSocket;
	private TestApduHandler handler;
	private boolean isActive;
	private boolean isRunning = false;
	private Socket clientSocket;

	public TestSocketSimComm(ServerSocket serverSocket) throws IOException {
		this.serverSocket = serverSocket;
		System.out.println("Starting TestSocketSim on port: " + serverSocket.getLocalPort());
	}

	@Override
	public void run() {
		clientSocket = null;
		isActive = true;
		isRunning = true;
		try {
			clientSocket = serverSocket.accept();
			System.out.println("New TestSocketSim connection on port: "
					+ clientSocket.getLocalPort());

			BufferedReader in = new BufferedReader(new InputStreamReader(
					clientSocket.getInputStream()));
			PrintStream out = new PrintStream(clientSocket.getOutputStream());

			do {
				String apduLine = in.readLine();
				if (apduLine == null) {
					break;
				}
				System.out.println("TestSocketSim received apdu: " + apduLine);
				String result = "";
				if (handler != null) {
					result = handler.processCommand(apduLine);

				}
				System.out.println("TestSocketSim sent response: " + result);
				out.println(result);

				out.flush();

			} while (isActive);
			clientSocket.close();
		} catch (SocketException e) {
			// expected behavior if closed
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Stops the server and disconnects all clients.
	 * 
	 * @throws IOException
	 */
	public void stop() throws IOException {
		isActive = false;
		if (clientSocket != null) {
			clientSocket.close();
		}
		serverSocket.close();
		isRunning = false;
	}

	/**
	 * @return true, iff the communication loop is running
	 */
	public boolean isRunning() {
		return isRunning;
	}

	/**
	 * Sets a new handler for APDU processing.
	 * 
	 * @param handler
	 */
	public void setHandler(TestApduHandler handler) {
		this.handler = handler;
	}

}
