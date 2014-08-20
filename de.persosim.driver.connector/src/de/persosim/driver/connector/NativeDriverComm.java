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
	private BufferedWriter bufferedEventOut;
	private Socket dataSocket;
	private Socket eventSocket;

	public NativeDriverComm(String hostName, int dataPort, int eventPort, Collection<PcscListener> listeners)
			throws IOException {
		this.listeners = listeners;
		this.dataSocket = new Socket(hostName, dataPort);
		this.eventSocket = new Socket(hostName, eventPort);
		bufferedDataIn = new BufferedReader(new InputStreamReader(
				dataSocket.getInputStream()));
		bufferedDataOut = new BufferedWriter(new OutputStreamWriter(
				dataSocket.getOutputStream()));
		bufferedEventOut = new BufferedWriter(new OutputStreamWriter(
				eventSocket.getOutputStream()));
	}

	public void sendEventToDriver(String data) throws IOException {
		bufferedEventOut.write(data);
		bufferedEventOut.newLine();
		bufferedEventOut.flush();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		try {
			while (!this.isInterrupted()) {
				try {
					while (!bufferedDataIn.ready()){
						Thread.yield();
					}
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
		} finally {
			try {
				dataSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				eventSocket.close();
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
		// TODO Auto-generated method stub
		super.interrupt();
		if (dataSocket != null){
			try {
				dataSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (eventSocket != null){
			try {
				eventSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
