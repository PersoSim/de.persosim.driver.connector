package de.persosim.driver.connector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;

import de.persosim.driver.connector.CommUtils.HandshakeMode;
import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.connector.pcsc.PcscCallResult;
import de.persosim.driver.connector.pcsc.PcscConstants;
import de.persosim.driver.connector.pcsc.PcscListener;
import de.persosim.simulator.utils.HexString;

/**
 * This reads PCSC data from the native interface and delegates it to the @link
 * {@link PcscListener} instances.
 * 
 * @author mboonk
 * 
 */
public class VirtualDriverComm implements NativeDriverComm {
	
	public static final String DEFAULT_HOST = "localhost";
	public static final int DEFAULT_PORT = 5678;
	
	
	private List<PcscListener> listeners;
	private Socket dataSocket;
	private UnsignedInteger lun;
	private boolean isRunning = true;
	private boolean isConnected = false;
	private Thread driverComm;

	/**
	 * Constructor using the connection information.
	 * 
	 * @param socket
	 *            the socket to use for communication with the native driver
	 * @throws IOException
	 */
	public VirtualDriverComm(Socket socket) throws IOException {
		this.dataSocket = socket;
	}

	public void log(PcscCallResult data) {
		BasicLogger.log(getClass(), "PCSC Out:\t" + data.getEncoded(), LogLevel.TRACE);
	}

	public void log(PcscCallData data) {
		String logmessage = "PCSC In:\t" + getStringRep(data.getFunction()) + NativeDriverInterface.MESSAGE_DIVIDER + data.getLogicalUnitNumber().getAsHexString();
		for (byte[] current : data.getParameters()) {
			logmessage += System.lineSeparator() + NativeDriverInterface.MESSAGE_DIVIDER
					+ HexString.encode(current);
		}
		BasicLogger.log(this.getClass(), logmessage, LogLevel.TRACE);
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
	 * @return true, iff the this object is connected to a native driver and has
	 *         successfully executed the handshake
	 */
	public boolean isConnected() {
		return isConnected;
	}

	@Override
	public void start() {		
		driverComm = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try (BufferedReader bufferedDataIn = new BufferedReader(new InputStreamReader(
							dataSocket.getInputStream()));
					BufferedWriter bufferedDataOut = new BufferedWriter(new OutputStreamWriter(
							dataSocket.getOutputStream()));){
					
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
									if (listeners != null) {
										for (PcscListener listener : listeners) {
											try {
												PcscCallResult currentResult = listener
														.processPcscCall(callData);
												if (result == null && currentResult != null) {
													// ignore all but the first result
													result = currentResult;
												}
											} catch (RuntimeException e) {
												BasicLogger.logException(getClass(),
														"Something went wrong while processing of the PCSC data by listener \""
																+ listener.getClass().getName() + "\"!\"",
														e, LogLevel.ERROR);
											}
										}										
									} else {
										BasicLogger.log(getClass(), "No PCSC listeners registered!", LogLevel.WARN);
									}
								} catch (RuntimeException e) {
									BasicLogger.logException(getClass(),
											"Something went wrong while parsing the PCSC data!",
											e, LogLevel.ERROR);
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
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		
		driverComm.start();
		
	}

	@Override
	public void stop() {
		isRunning = false;

		try {
			dataSocket.close();
		} catch (IOException e) {
			BasicLogger.logException("Could not close data socket", e, LogLevel.ERROR);
		}

		Socket temp;
		try {
			temp = new Socket(dataSocket.getInetAddress(), dataSocket.getPort());
			CommUtils.doHandshake(temp, lun, HandshakeMode.CLOSE);
			temp.close();

			// Hack (wait for pcsc to poll and kill connections until the driver
			// handles the closure handshakes)
			Thread.sleep(3000);
		} catch (IOException|InterruptedException e) {
			BasicLogger.logException("Could not perform closing handshake", e, LogLevel.ERROR);
		}
		

		try {
			driverComm.join();
		} catch (InterruptedException e) {
			BasicLogger.logException("Waiting for communication thread join failed", e, LogLevel.ERROR);
		}
		
		isConnected = false;
	}

	@Override
	public boolean isRunning() {
		return isRunning;
	}

	@Override
	public void setListeners(List<PcscListener> listeners) {
		this.listeners = listeners;
	}

}
