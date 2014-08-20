package de.persosim.driver.connector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Collection;

import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.connector.pcsc.PcscCallResult;
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
	
	public static final String MESSAGE_ICC_HELLO = "HELLO_ICC";	//Example: HELLO_ICC|LUN:-1
	public static final String MESSAGE_ICC_STOP = "STOP_ICC";
	public static final String MESSAGE_ICC_ERROR = "ERROR_ICC";
	public static final String MESSAGE_ICC_DONE = "DONE_ICC";
	public static final String MESSAGE_IFD_HELLO = "HELLO_IFD"; //Example: HELLO_IFD|LUN:3
	public static final String MESSAGE_IFD_DONE = "DONE_IFD";
	public static final String MESSAGE_IFD_ERROR = "ERROR_IFD";

	public NativeDriverComm(String hostName, int dataPort, Collection<PcscListener> listeners)
			throws IOException {
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
			CommUtils.doHandshake(dataSocket);
			while (!this.isInterrupted()) {
				try {
					String data = bufferedDataIn.readLine();
					PcscCallResult result = null;
					for (PcscListener listener : listeners) {
						PcscCallResult currentResult = listener
								.processPcscCall(new PcscCallData(data));
						if (result == null && currentResult != null) {
							// ignore all but the first result
							result = currentResult;
						}
					}
					bufferedDataOut.write(result.getEncoded());
					bufferedDataOut.newLine();
					bufferedDataOut.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} finally {
			try {
				dataSocket.close();
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
		try {
			dataSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
}
