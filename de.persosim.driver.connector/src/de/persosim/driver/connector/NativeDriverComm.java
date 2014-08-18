package de.persosim.driver.connector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
	private BufferedReader bufferedIn;

	public NativeDriverComm(Socket socket,
			Collection<PcscListener> listeners) throws IOException {
		this.listeners = listeners;
		bufferedIn = new BufferedReader(new InputStreamReader(
				socket.getInputStream()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		while (!this.isInterrupted()) {
			try {
				String data = bufferedIn.readLine();
				PcscCallResult result = null;
				for (PcscListener listener : listeners) {
					PcscCallResult currentResult = listener.processPcscCall(new PcscCallData(data));
					if (result == null && currentResult != null){
						//ignore all but the first result
						result = currentResult;
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
