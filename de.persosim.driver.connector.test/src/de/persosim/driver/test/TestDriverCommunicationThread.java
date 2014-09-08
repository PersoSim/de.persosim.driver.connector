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
import java.util.HashMap;

import de.persosim.driver.connector.NativeDriverInterface;

/**
 * This {@link Thread} communicates for the test driver and manages the
 * connections.
 * 
 * @author mboonk
 *
 */
public class TestDriverCommunicationThread extends Thread implements
		NativeDriverInterface {

	private ServerSocket serverSocket;
	private HashMap<Integer, Socket> lunMapping;
	private HashMap<Integer, HandshakeData> lunHandshakeData;
	private HandshakeData currentHandshake;
	private Socket clientSocket;
	private static final int MAX_LUNS = 10;

	private int getLowestFreeLun() {

		for (int i = 0; i < MAX_LUNS; i++) {
			if (!lunMapping.containsKey(i))
				return 0;
		}
		return -1;
	}

	public TestDriverCommunicationThread(int port,
			HashMap<Integer, Socket> lunMapping,
			Collection<DriverEventListener> listeners) throws IOException {
		this.setName("TestDriverCommunicationThread");
		this.lunMapping = lunMapping;
		lunHandshakeData = new HashMap<Integer, HandshakeData>();

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
				System.out.println("New local socket on port: "
						+ clientSocket.getPort());
				BufferedReader bufferedIn = new BufferedReader(
						new InputStreamReader(clientSocket.getInputStream()));

				String data = null;
				boolean handshakeDone = false;
				while (!handshakeDone && (data = bufferedIn.readLine()) != null) {
					BufferedWriter bufferedOut = new BufferedWriter(
							new OutputStreamWriter(
									clientSocket.getOutputStream()));
					System.out.println("Data from ICC:\t\t" + data);
					String dataToSend = doHandshake(data);
					bufferedOut.write(dataToSend);
					bufferedOut.newLine();
					bufferedOut.flush();
					System.out.println("Data sent to ICC:\t" + dataToSend);
					if (dataToSend.equals(MESSAGE_IFD_DONE)) {
						if (data.equals(MESSAGE_ICC_STOP)) {
							clientSocket.close();
							System.out.println("Local socket on port "
									+ clientSocket.getPort() + " closed.");
						}
						handshakeDone = true;
					}
				}
			} catch (SocketException e) {
				// ignore, expected behaviour on interrupt
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private String doHandshake(String data) {
		String[] split = data.split("\\|");
		if (split.length > 0) {
			switch (split[0]) {
			case MESSAGE_ICC_HELLO:
				if (split.length > 1) {
					String[] subCommand = split[1].split(":");
					int lun = -1;
					if (subCommand.length > 1 && subCommand[0].equals("LUN")) {
						lun = Integer.parseInt(subCommand[1]);
						if (lun > MAX_LUNS) {
							return MESSAGE_IFD_ERROR;
						}
						if (lun < 0) {
							lun = getLowestFreeLun();
						}
					}
					if (lunMapping.containsKey(lun)) {
						try {
							lunMapping.get(lun).close();
							System.out.println("Closed socket on port: "
									+ lunMapping.get(lun).getPort());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					lunMapping.put(lun, clientSocket);
					if (lunHandshakeData.containsKey(lun)) {
						currentHandshake = lunHandshakeData.get(lun);
					} else {
						currentHandshake = new HandshakeData();
						currentHandshake.setLun(lun);
						lunHandshakeData.put(lun, currentHandshake);
					}
					return MESSAGE_IFD_HELLO + "|LUN:" + lun;
				}
				break;
			case MESSAGE_ICC_STOP:
				int lun = currentHandshake.getLun();
				if (lunHandshakeData.containsKey(lun)
						&& !lunHandshakeData.get(lun).isHandshakeDone()) {
					return MESSAGE_IFD_ERROR;
				}
				lunHandshakeData.remove(lun);
				currentHandshake = null;
				return MESSAGE_IFD_DONE;

			case MESSAGE_ICC_DONE:
				currentHandshake.setHandshakeDone(true);
				return MESSAGE_IFD_DONE;
			}
		}
		return MESSAGE_ICC_ERROR;
	}

	public int getPort() {
		return serverSocket.getLocalPort();
	}

	/*
	 * (non-Javadoc)
	 * 
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
