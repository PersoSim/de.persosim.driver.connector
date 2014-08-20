package de.persosim.driver.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

/**
 * This class can be used for testcases that need an open native driver server
 * socket for testing.
 * 
 * Protocol:
 * 
 * Messages are of the form:
 * 
 * function|param:value|param2:value2
 * 
 * An initial Handshake consists of 4 messages:
 * 
 * HELLO_ICC|LUN:-1			if this is a new connection, if not LUN is the last used lun
 * HELLO_IFD|LUN:x			the IFD answers with the new lun as integer
 * DONE_ICC
 * DONE_ICC
 * 
 * To stop a channel the handshake is slightly different:
 * 
 * HELLO_ICC|LUN:x
 * HELLO_IFD|LUN:x
 * STOP_ICC
 * DONE_IFD
 * 
 * @author mboonk
 * 
 */
public class TestDriver {
	public static final int PORT_NUMBER_DEFAULT = 5678;
	
	private Collection<DriverEventListener> listeners = new HashSet<DriverEventListener>();

	private TestDriverCommunicationThread communicationThread;
	private HashMap<Integer, Socket> lunMapping = new HashMap<>();
	
	private boolean running = false;
	
	public void start() throws IOException{
		start(PORT_NUMBER_DEFAULT);
	}
	
	public void start(int port) throws IOException{
		if (!running){
			System.out.println("Starting test driver on port " + port);
			communicationThread = new TestDriverCommunicationThread(port, lunMapping, listeners);
			communicationThread.setDaemon(true);
			communicationThread.start();
			
			running = true;
		} else {
			System.out.println("This test driver instance is already runnning");
		}
	}

	public void stop() throws InterruptedException {
		if (running){
			System.out.println("Stopping test driver on port " + communicationThread.getPort());
			communicationThread.interrupt();
			running = false;
		} else {
			System.out.println("This test driver instance is not running");
		}
	}

	public String sendData(int lun, String data) throws IOException, InterruptedException{
		Socket iccSocket = lunMapping.get(lun);
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

	public void addListener(DriverEventListener listener) {
		listeners.add(listener);
	}

	public void removeListener(DriverEventListener listener) {
		listeners.remove(listener);
	}
}
