package de.persosim.driver.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;

public class EventThread extends Thread {

	private Collection<DriverEventListener> listeners;
	private ServerSocket serverSocket;
	private Socket clientSocket;

	public EventThread(int port, Collection<DriverEventListener> listeners)
			throws IOException {
		this.setName("EventThread");
		this.listeners = listeners;
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		clientSocket = null;
		try {
			while (!isInterrupted()) {
				try {
					clientSocket = serverSocket.accept();
					BufferedReader bufferedIn = new BufferedReader(
							new InputStreamReader(clientSocket.getInputStream()));

					String data = null;
					while(!bufferedIn.ready())
			        {
			            Thread.yield();
			        }
					
					while ((data = bufferedIn.readLine()) != null) {
						for (DriverEventListener listener : listeners) {
							listener.eventReceivedCallback(data);
						}
					}
				} catch (SocketException e){
					//ignore, expected behaviour on interrupt
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} finally {
			try {
				if (clientSocket != null){
					clientSocket.close();	
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				if (serverSocket != null){
					serverSocket.close();
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#interrupt()
	 */
	@Override
	public void interrupt() {
		super.interrupt();
		if (serverSocket != null){
			try {
				serverSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (clientSocket != null){
			try {
				clientSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
