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
import org.globaltester.logging.tags.LogTag;

import de.persosim.driver.connector.CommUtils.HandshakeMode;
import de.persosim.driver.connector.exceptions.IfdCreationException;
import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.connector.pcsc.PcscCallResult;
import de.persosim.driver.connector.pcsc.PcscConstants;
import de.persosim.driver.connector.pcsc.PcscListener;
import de.persosim.driver.connector.pcsc.SimplePcscCallResult;
import de.persosim.simulator.log.PersoSimLogTags;
import de.persosim.simulator.utils.HexString;

/**
 * This reads PCSC data from the native interface and delegates it to the @link
 * {@link PcscListener} instances.
 *
 * @author mboonk
 *
 */
public class VirtualDriverComm implements IfdComm, Runnable
{

	public static final String DEFAULT_HOST = "localhost";
	public static final int DEFAULT_PORT = 5678;
	public static final String NAME = "VIRTUAL";

	private List<PcscListener> listeners;
	private Socket dataSocket;
	private UnsignedInteger lun;
	private boolean isRunning = false;
	private boolean isConnected = false;
	private Thread driverComm;
	private String host;
	private int port;

	/**
	 * Constructor using the connection information.
	 *
	 * @param socket
	 *            the socket to use for communication with the native driver
	 * @throws IfdCreationException
	 * @throws IOException
	 */
	public VirtualDriverComm(String host, int port) throws IfdCreationException
	{
		this.host = host;
		this.port = port;

		try {
			dataSocket = new Socket(host, port);
			dataSocket.close();
		}
		catch (IOException e) {
			throw new IfdCreationException("Can not connect to virtual driver", e);
		}
	}

	public void log(PcscCallResult data)
	{
		BasicLogger.log("PCSC Out:\t" + data.getEncoded(), LogLevel.TRACE, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.VIRTUAL_DRIVER_TAG_ID));
	}

	public void log(PcscCallData data)
	{
		StringBuilder logmessage = new StringBuilder();
		logmessage.append("PCSC In:\t").append(getStringRep(data.getFunction())).append(IfdInterface.MESSAGE_DIVIDER).append(data.getLogicalUnitNumber().getAsHexString());
		for (byte[] current : data.getParameters()) {
			logmessage.append(System.lineSeparator()).append(IfdInterface.MESSAGE_DIVIDER).append(HexString.encode(current));
		}
		BasicLogger.log(logmessage.toString(), LogLevel.TRACE, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.VIRTUAL_DRIVER_TAG_ID));
	}

	String getStringRep(UnsignedInteger value)
	{
		Field[] fields = IfdInterface.class.getDeclaredFields();
		for (Field field : fields) {
			try {
				if (value.equals(field.get(null))) {
					return field.getName();
				}
			}
			catch (Exception e) {
				// do nothing
			}
		}
		return value.getAsHexString();
	}

	/**
	 * @return true, iff the this object is connected to a native driver and has
	 *         successfully executed the handshake
	 */
	public boolean isConnected()
	{
		return isConnected;
	}

	@Override
	public void start()
	{
		try {
			dataSocket = new Socket(host, port);
		}
		catch (IOException e) {
			throw new IllegalStateException("Socket could not be created", e);
		}
		driverComm = new Thread(this);
		driverComm.start();
	}

	@Override
	public void stop()
	{
		BasicLogger.log("Stopping driver comm", LogLevel.TRACE, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.VIRTUAL_DRIVER_TAG_ID));
		if (!isRunning) {
			return;
		}

		driverComm.interrupt();

		try {
			dataSocket.close();
		}
		catch (IOException e) {
			BasicLogger.logException("Could not close data socket", e, LogLevel.ERROR, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.VIRTUAL_DRIVER_TAG_ID));
		}

		Socket temp;
		try {
			temp = new Socket(dataSocket.getInetAddress(), dataSocket.getPort());
			CommUtils.doHandshake(temp, lun, HandshakeMode.CLOSE);
			temp.close();

			// Hack (wait for pcsc to poll and kill connections until the driver
			// handles the closure handshakes)
			Thread.sleep(3000);
		}
		catch (IOException | InterruptedException e) {
			BasicLogger.logException("Could not perform closing handshake", e, LogLevel.ERROR, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.VIRTUAL_DRIVER_TAG_ID));
		}

		try {
			driverComm.join();
		}
		catch (InterruptedException e) {
			BasicLogger.logException("Waiting for communication thread join failed", e, LogLevel.ERROR, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.VIRTUAL_DRIVER_TAG_ID));
		}

		isConnected = false;
		dataSocket = null;
		driverComm = null;
		isRunning = false;
		BasicLogger.log("Stopping driver comm done", LogLevel.TRACE, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.VIRTUAL_DRIVER_TAG_ID));
	}

	@Override
	public boolean isRunning()
	{
		return isRunning;
	}

	@Override
	public void setListeners(List<PcscListener> listeners)
	{
		this.listeners = listeners;
	}

	@Override
	public void run()
	{
		isRunning = true;
		try (BufferedReader bufferedDataIn = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()));
				BufferedWriter bufferedDataOut = new BufferedWriter(new OutputStreamWriter(dataSocket.getOutputStream()));) {

			lun = CommUtils.doHandshake(dataSocket, HandshakeMode.OPEN);
			isConnected = true;
			while (!Thread.interrupted()) {
				processData(bufferedDataIn, bufferedDataOut);
			}
		}
		catch (Exception e1) {
			BasicLogger.logException(e1.getMessage(), e1, LogLevel.ERROR, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.VIRTUAL_DRIVER_TAG_ID));

		}
	}

	private void processData(BufferedReader bufferedDataIn, BufferedWriter bufferedDataOut) throws IOException
	{
		try {
			String data = bufferedDataIn.readLine();
			PcscCallResult result = null;
			if (data != null) {
				result = processCalls(data, result);
				if (result == null) {
					result = new SimplePcscCallResult(PcscConstants.IFD_NOT_SUPPORTED);
				}

				log(result);

				bufferedDataOut.write(result.getEncoded());
				bufferedDataOut.newLine();
				bufferedDataOut.flush();
			}
		}
		catch (SocketException e) {
			// expected behavior after interrupting
		}
	}

	private PcscCallResult processCalls(String data, PcscCallResult result)
	{
		try {
			PcscCallData callData = new PcscCallData(data);
			log(callData);
			if (listeners != null) {
				for (PcscListener listener : listeners) {
					result = processCall(result, callData, listener);
				}
			}
			else {
				BasicLogger.log("No PCSC listeners registered!", LogLevel.WARN, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.VIRTUAL_DRIVER_TAG_ID));
			}
		}
		catch (RuntimeException e) {
			BasicLogger.logException("Something went wrong while parsing the PCSC data!", e, LogLevel.ERROR, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.VIRTUAL_DRIVER_TAG_ID));
		}
		return result;
	}

	private PcscCallResult processCall(PcscCallResult result, PcscCallData callData, PcscListener listener)
	{
		try {
			PcscCallResult currentResult = listener.processPcscCall(callData);
			if (result == null && currentResult != null) {
				// ignore all but the first result
				result = currentResult;
			}
		}
		catch (RuntimeException e) {
			BasicLogger.logException("Something went wrong while processing of the PCSC data by listener \"" + listener.getClass().getName() + "\"!\"", e, LogLevel.ERROR,
					new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.VIRTUAL_DRIVER_TAG_ID));
		}
		return result;
	}

	@Override
	public void reset()
	{
		if (isRunning) {
			stop();
		}
		listeners = null;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getUserString()
	{
		return "Virtual PCSClite driver";
	}

}
