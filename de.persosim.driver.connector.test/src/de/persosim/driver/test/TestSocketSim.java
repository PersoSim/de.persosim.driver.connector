package de.persosim.driver.test;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * This class provides a simulation of the PersoSim socket simulator adaptor by
 * opening a server socket on the given port. The answering behavior is defined
 * by setting a {@link TestApduHandler}.
 * 
 * @author mboonk
 *
 */
public class TestSocketSim {
	private Thread communicationThread;
	private TestSocketSimComm communication;
	private boolean isRunning;
	
	/**
	 * Creates the server and starts listening for connections.
	 * 
	 * @throws IOException
	 */
	public void start(ServerSocket serverSocket) throws IOException {
		communication = new TestSocketSimComm(serverSocket);
		communicationThread = new Thread(communication);
		communicationThread.start();
		while (!communication.isRunning()) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// log only,as we are in test code here
				e.printStackTrace();
			}
		}

		isRunning = true;
	}

	/**
	 * Sets a new handler for APDU processing, the old handler is replaced.
	 * 
	 * @param handler
	 */
	public void setHandler(TestApduHandler handler) {
		if (communication != null) {
			communication.setHandler(handler);
		} else {
			throw new NullPointerException(
					"The communication thread is not running, therefore no handler can be set");
		}
	}

	/**
	 * Stops the server.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void stop() throws IOException, InterruptedException {
		if (isRunning) {
			communication.stop();
			communicationThread.join();
		}
	}

}
