package de.persosim.driver.connector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;

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
public class NativeDriverComm extends Thread {
	private Collection<PcscListener> listeners;
	private BufferedReader bufferedDataIn;
	private BufferedWriter bufferedDataOut;
	private Socket dataSocket;

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
			Collection<PcscListener> listeners) throws IOException {
		this.listeners = listeners;
		this.dataSocket = new Socket(hostName, dataPort);
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
			CommUtils.doHandshake(dataSocket, HandshakeMode.OPEN);
			while (!this.isInterrupted()) {
				try {
					String data = bufferedDataIn.readLine();
					PcscCallResult result = null;
					if (data != null) {
						try {
							PcscCallData callData = new PcscCallData(data);
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
									return PcscConstants.IFD_NOT_SUPPORTED.getAsHexString();
								}
							};
						}

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#interrupt()
	 */
	@Override
	public void interrupt() {
		super.interrupt();
		try {
			dataSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
