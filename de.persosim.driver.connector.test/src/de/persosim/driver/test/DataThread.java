package de.persosim.driver.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;

public class DataThread extends Thread {

	private Collection<DriverEventListener> listeners;
	private ServerSocket serverSocket;
	private Socket iccSocket;

	public DataThread(int port, Collection<DriverEventListener> listeners)
			throws IOException {
		this.setName("DataThread");
		this.listeners = listeners;

		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String sendData(String data) throws IOException, InterruptedException{
		if (iccSocket != null && iccSocket.isConnected()){
			System.out.println("Driver sending data to sim");
			BufferedWriter bufferedOut = new BufferedWriter(new OutputStreamWriter(iccSocket.getOutputStream()));
			bufferedOut.write(data);
			bufferedOut.newLine();
			bufferedOut.flush();
			
			BufferedReader bufferedIn = new BufferedReader(
					new InputStreamReader(iccSocket.getInputStream()));
			System.out.println("Driver waiting for response");
			while(!bufferedIn.ready())
	        {
	            Thread.yield();
	        }
			
			data = bufferedIn.readLine();
			for (DriverEventListener listener : listeners) {
				System.out.println("Notifing listener of type " + listener.getClass().getSimpleName());
				listener.messageReceivedCallback(data);
			}
			return data;
		}
		return null;
	}
	
	@Override
	public void run() {
			while (!isInterrupted()) {
				Socket newSocket = null;
				try {
					newSocket = serverSocket.accept();
					iccSocket = newSocket;
				} catch (SocketException e){
					//expected behavior on interrupt
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
		if (iccSocket != null){
			try {
				iccSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
