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
import de.persosim.simulator.utils.HexString;

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
	private HashMap<Byte, Socket> lunMapping;
	private HashMap<Byte, HandshakeData> lunHandshakeData;
	private HandshakeData currentHandshake;
	private Socket clientSocket;
	private static final byte MAX_LUNS = 10;

	private byte getLowestFreeLun() {

		for (byte i = 0; i < MAX_LUNS; i++) {
			if (!lunMapping.containsKey(i))
				return 0;
		}
		return -1;
	}

	public TestDriverCommunicationThread(int port,
			HashMap<Byte, Socket> lunMapping,
			Collection<DriverEventListener> listeners) throws IOException {
		this.setName("TestDriverCommunicationThread");
		this.lunMapping = lunMapping;
		lunHandshakeData = new HashMap<Byte, HandshakeData>();

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
					System.out.println("TestDriver received data from the connector:\t" + data);
					String dataToSend = doHandshake(data);

					if (dataToSend == null) {
						if (data.equals(HexString.encode(MESSAGE_ICC_STOP))) {
							clientSocket.close();
							System.out.println("Local socket on port "
									+ clientSocket.getPort() + " closed.");
						}
						handshakeDone = true;
					} else {
						bufferedOut.write(dataToSend);
						bufferedOut.newLine();
						bufferedOut.flush();
					}
					
					System.out.println("TestDriver sent data to the connector:\t" + dataToSend);
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
			switch ((byte) Integer.parseInt(split[0], 16)) {
			case MESSAGE_ICC_HELLO:
				if (split.length > 1) {
					byte lun = -1;
					if (split.length > 1) {
						lun = (byte) Integer.parseInt(split[1], 16);
						if (lun > MAX_LUNS) {
							return HexString.encode(MESSAGE_IFD_ERROR);
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
					return HexString.encode(MESSAGE_IFD_HELLO) + "|" + HexString.encode(lun);
				}
				break;
			case MESSAGE_ICC_STOP:
				int lun = currentHandshake.getLun();
				if (lunHandshakeData.containsKey(lun)
						&& !lunHandshakeData.get(lun).isHandshakeDone()) {
					return HexString.encode(MESSAGE_IFD_ERROR);
				}
				lunHandshakeData.remove(lun);
				currentHandshake = null;
				return null;

			case MESSAGE_ICC_DONE:
				currentHandshake.setHandshakeDone(true);
				return null;
			}
		}
		return HexString.encode(MESSAGE_ICC_ERROR);
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
