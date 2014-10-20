package de.persosim.driver.connector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;
import java.util.List;

import de.persosim.driver.connector.CommUtils.HandshakeMode;
import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.connector.pcsc.PcscCallResult;
import de.persosim.driver.connector.pcsc.PcscConstants;
import de.persosim.driver.connector.pcsc.PcscListener;

/**
 * This thread reads PCSC data from the native interface and delegates it to the @link
 * {@link PcscListener} instances.
 * 
 * @author mboonk
 * 
 */
public class NativeDriverComm implements Runnable {
	private List<PcscListener> listeners;
	private BufferedReader bufferedDataIn;
	private BufferedWriter bufferedDataOut;
	private Socket dataSocket;
	private UnsignedInteger lun;
	private boolean isRunning = true;
	private boolean isConnected = false;

	String hostName;
	int dataPort;

	/**
	 * Constructor using the connection information and a {@link Collection} of
	 * listeners.
	 * 
	 * @param hostName
	 * @param dataPort
	 * @param listeners
	 *            the {@link PcscListener}s that need to be informed when a new
	 *            PCSC message is transmitted
	 * @throws IOException
	 */
	public NativeDriverComm(String hostName, int dataPort,
			List<PcscListener> listeners) throws IOException {
		this.listeners = listeners;
		this.dataSocket = new Socket(hostName, dataPort);
		this.hostName = hostName;
		this.dataPort = dataPort;
		bufferedDataIn = new BufferedReader(new InputStreamReader(
				dataSocket.getInputStream()));
		bufferedDataOut = new BufferedWriter(new OutputStreamWriter(
				dataSocket.getOutputStream()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		try {
			isRunning = true;
			lun = CommUtils.doHandshake(dataSocket, HandshakeMode.OPEN);
			isConnected = true;
			while (isRunning) {
				try {
					String data = bufferedDataIn.readLine();
					PcscCallResult result = null;
					if (data != null) {
						try {
							PcscCallData callData = new PcscCallData(data);
							log(callData);
							for (PcscListener listener : listeners) {
								try {
									PcscCallResult currentResult = listener
											.processPcscCall(callData);
									if (result == null && currentResult != null) {
										// ignore all but the first result
										result = currentResult;
									}
								} catch (RuntimeException e) {
									System.out
											.println("Something went wrong while processing of the PCSC data by listener "
													+ listener.getClass()
															.getName() + "!");
									e.printStackTrace();
								}
							}
						} catch (RuntimeException e) {
							System.out
									.println("Something went wrong while parsing the PCSC data!");
							e.printStackTrace();
						}
						if (result == null) {
							result = new PcscCallResult() {
								@Override
								public String getEncoded() {
									return PcscConstants.IFD_NOT_SUPPORTED
											.getAsHexString();
								}
							};
						}

						log(result);

						bufferedDataOut.write(result.getEncoded());
						bufferedDataOut.newLine();
						bufferedDataOut.flush();
					}
				} catch (SocketException e) {
					// expected behavior after interrupting
				}
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public void log(PcscCallResult data) {
		//TODO use proper logging mechanism
		//System.out.println("PCSC Out:\t" + data.getEncoded());
	}

	public void log(PcscCallData data) {
		//TODO use proper logging mechanism
		/*System.out.print("PCSC In:\t" + getStringRep(data.getFunction()) + NativeDriverInterface.MESSAGE_DIVIDER + data.getLogicalUnitNumber().getAsHexString());
		for (byte[] current : data.getParameters()) {
			System.out.print(NativeDriverInterface.MESSAGE_DIVIDER
					+ HexString.encode(current));
		}
		System.out.println();*/
	}

	String getStringRep(UnsignedInteger value) {
		Field[] fields = NativeDriverInterface.class.getDeclaredFields();
		for (Field field : fields) {
			try {
				if (value.equals(field.get(new NativeDriverInterface() {
				}))) {
					return field.getName();
				}
			} catch (Exception e) {
				continue;
			}
		}
		return value.getAsHexString();
	}

	/**
	 * Disconnects the communication thread from the native driver and does a
	 * closure handshake.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void disconnect() throws IOException, InterruptedException {
		isRunning = false;

		dataSocket.close();

		Socket temp = new Socket(hostName, dataPort);
		CommUtils.doHandshake(temp, lun, HandshakeMode.CLOSE);
		dataSocket.close();
		temp.close();

		// XXX Hack (wait for pcsc to poll and kill connections until the driver
		// handles the closure handshakes)
		Thread.sleep(3000);
		isConnected = false;
	}

	/**
	 * @return true, iff the this object is connected to a native driver and has
	 *         successfully executed the handshake
	 */
	public boolean isConnected() {
		return isConnected;
	}

}
