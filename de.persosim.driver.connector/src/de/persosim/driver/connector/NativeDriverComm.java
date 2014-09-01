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

	public static final String MESSAGE_ICC_HELLO = "HELLO_ICC"; // Example:
																// HELLO_ICC|LUN:-1
	public static final String MESSAGE_ICC_STOP = "STOP_ICC";
	public static final String MESSAGE_ICC_ERROR = "ERROR_ICC";
	public static final String MESSAGE_ICC_DONE = "DONE_ICC";
	public static final String MESSAGE_IFD_HELLO = "HELLO_IFD"; // Example:
																// HELLO_IFD|LUN:3
	public static final String MESSAGE_IFD_DONE = "DONE_IFD";
	public static final String MESSAGE_IFD_ERROR = "ERROR_IFD";

	public static final String TYPE_STR = "STR";
	public static final String TYPE_DWORD = "DWORD";
	public static final String TYPE_TLV = "TLV";
	public static final String TYPE_BYTE_ARRAY = "BYTE_ARRAY";
	public static final int PCSC_FUNCTION_DEVICE_CONTROL = 0;
	public static final int PCSC_FUNCTION_DEVICE_LIST_DEVICES = 1;
	public static final int PCSC_FUNCTION_GET_CAPABILITIES = 2;
	public static final int PCSC_FUNCTION_SET_CAPABILITIES = 3;
	public static final int PCSC_FUNCTION_POWER_ICC = 4;
	public static final int PCSC_FUNCTION_TRANSMIT_TO_ICC = 5;

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
			CommUtils.doHandshake(dataSocket, -1, HandshakeMode.OPEN);
			while (!this.isInterrupted()) {
				try {
					String data = bufferedDataIn.readLine();
					PcscCallResult result = null;
					if (data != null){
						try{
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
										.println("Something went wrong while processing of the PCSC data by listener " + listener.getClass().getName() + "!");
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
									return "" + PcscConstants.IFD_NOT_SUPPORTED;
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
