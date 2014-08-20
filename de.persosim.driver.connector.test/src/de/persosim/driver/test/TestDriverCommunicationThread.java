package de.persosim.driver.test;

import static de.persosim.driver.connector.NativeDriverComm.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;
import java.util.HashMap;

public class TestDriverCommunicationThread extends Thread {

	private ServerSocket serverSocket;
	private HashMap<Integer, Socket> lunMapping;
	private Socket clientSocket;
	private static final int MAX_LUNS = 10;
	
	private int getLowestFreeLun(){
		
		for (int i = 0; i < MAX_LUNS; i++){
			if (!lunMapping.containsKey(i)) return 0;
		}
		return -1;
	}

	public TestDriverCommunicationThread(int port, HashMap<Integer, Socket> lunMapping, Collection<DriverEventListener> listeners)
			throws IOException {
		this.setName("TestDriverCommunicationThread");
		this.lunMapping = lunMapping;

		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	
	@Override
	public void run() {
		while (!isInterrupted()) {
			try {
				clientSocket = serverSocket.accept();
				BufferedReader bufferedIn = new BufferedReader(
						new InputStreamReader(clientSocket.getInputStream()));

				String data = null;
				boolean handshakeDone = false;
				while (!handshakeDone && (data = bufferedIn.readLine()) != null) {
					BufferedWriter bufferedOut = new BufferedWriter(
							new OutputStreamWriter(
									clientSocket.getOutputStream()));
					String dataToSend = doHandshake(data);
					if (dataToSend.equals(MESSAGE_IFD_DONE)) {
						handshakeDone = true;
					}
					bufferedOut.write(dataToSend);
					bufferedOut.newLine();
					bufferedOut.flush();
				}
			} catch (SocketException e) {
				// ignore, expected behaviour on interrupt
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private String doHandshake(String data){
		String [] split = data.split("\\|");
		if (split.length > 0){
			switch (split[0]){
			case MESSAGE_ICC_HELLO:
			case MESSAGE_ICC_STOP:
				if (split.length > 1){
					String [] subCommand = split[1].split(":");
					int lun = -1;
					if (subCommand.length > 1 && subCommand[0].equals("LUN")){
						lun = Integer.parseInt(subCommand[1]);
						if (lun > MAX_LUNS){
							return MESSAGE_IFD_ERROR;
						}
						if (lun < 0){
							lun = getLowestFreeLun();
						}
					}
					if (lunMapping.containsKey(lun)){
						try {
							lunMapping.get(lun).close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					if (split[0].equals(MESSAGE_ICC_HELLO)){
						lunMapping.put(lun, clientSocket);
						return MESSAGE_IFD_HELLO + "|LUN:" + lun;
					} else if (split[0].equals(MESSAGE_ICC_STOP)){
						lunMapping.remove(lun);
						return MESSAGE_IFD_DONE;
					}
				}
				break;
			case MESSAGE_ICC_DONE:
				return MESSAGE_IFD_DONE;
			}	
		}
		return MESSAGE_ICC_ERROR;
	}

	public int getPort() {
		return serverSocket.getLocalPort();
	}

	/* (non-Javadoc)
	 * @see java.lang.Thread#interrupt()
	 */
	@Override
	public void interrupt() {
		super.interrupt();
		try {
			serverSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
