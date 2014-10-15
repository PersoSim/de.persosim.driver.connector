package de.persosim.driver.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import de.persosim.driver.connector.NativeDriverInterface;
import de.persosim.driver.connector.UnsignedInteger;
import de.persosim.simulator.utils.HexString;

/**
 * This class can be used for testcases that need an open native driver server
 * socket for testing.
 * 
 * @author mboonk
 * 
 */
public class TestDriver {
	public static final int PORT_NUMBER_DEFAULT = 5678;

	private Collection<DriverEventListener> listeners = new HashSet<DriverEventListener>();

	private TestDriverCommunication communicationThread;
	private HashMap<Integer, Socket> lunMapping = new HashMap<>();
	private int timeout = 2000;

	private boolean running = false;

	/**
	 * Start the test driver on the default port.
	 * 
	 * @throws IOException
	 */
	public void start() throws IOException {
		start(PORT_NUMBER_DEFAULT);
	}

	/**
	 * Start the test driver on the given port
	 * 
	 * @param port
	 * @throws IOException
	 */
	public void start(int port) throws IOException {
		if (!running) {
			System.out.println("Starting test driver on port " + port);
			communicationThread = new TestDriverCommunication(port,
					lunMapping, listeners);
			communicationThread.setDaemon(true);
			communicationThread.start();
			


			long timeOutTime = Calendar.getInstance().getTimeInMillis() + timeout;
			
			while (!communicationThread.isRunning()){
				if (Calendar.getInstance().getTimeInMillis() > timeOutTime){
					throw new IOException("The server thread has run into a timeout");
				}
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					throw new IOException("The communication thread was interrupted");
				}
			}
			running = true;
		} else {
			System.out.println("This test driver instance is already runnning");
		}
	}

	/**
	 * Stop the test driver execution. It can not be started again by running
	 * {@link #start()}
	 * 
	 * @throws InterruptedException
	 */
	public void stop() throws InterruptedException {
		if (running) {
			System.out.println("Stopping test driver on port "
					+ communicationThread.getPort());
			communicationThread.interrupt();
			communicationThread.join();
			running = false;
		} else {
			System.out.println("This test driver instance is not running");
		}
	}
	
	/**
	 * This method allows to directly send raw data over the socket to the
	 * connector at the given lun. The lun is NOT used for constructing a
	 * protocol compliant message.
	 * 
	 * @param lun
	 *            the lun to communicate with
	 * @param data
	 *            the data string to send
	 * @return the raw data returned as string
	 * @throws IOException
	 */
	public String sendDataDirect(UnsignedInteger lun, String data) throws IOException{
		Socket iccSocket = lunMapping.get(lun.getAsInt());
		if (iccSocket != null && iccSocket.isConnected()) {
			System.out.println("Driver sending data to connector");
			BufferedWriter bufferedOut = new BufferedWriter(
					new OutputStreamWriter(iccSocket.getOutputStream()));
			
			bufferedOut.write(data);
			bufferedOut.newLine();
			bufferedOut.flush();

			System.out.println("Driver sent PCSC data:\t\t" + data);
			
			BufferedReader bufferedIn = new BufferedReader(
					new InputStreamReader(iccSocket.getInputStream()));
			System.out.println("Driver waiting for response from connector");

			String responseData = bufferedIn.readLine();
			for (DriverEventListener listener : listeners) {
				System.out.println("Notifing listener of type "
						+ listener.getClass().getSimpleName());
				listener.messageReceivedCallback(responseData);
			}
			System.out.println("Driver received PCSC data:\t" + responseData);
			return responseData;
		}
		System.out.println("No connected socket found for lun: " + lun);
		return null;
	}

	/**
	 * Send correctly formated data from the test driver to simulate PCSC calls.
	 * 
	 * @param lun
	 * @param function
	 * @param data
	 * @return the resulting answer to the PCSC call or null if the data could not be sent
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public String sendData(UnsignedInteger lun, UnsignedInteger function, byte[]... data)
			throws IOException, InterruptedException {

		String dataToSend = function.getAsHexString() + NativeDriverInterface.MESSAGE_DIVIDER + lun.getAsHexString();

		for (byte[] current : data) {
			dataToSend += NativeDriverInterface.MESSAGE_DIVIDER + HexString.encode(current);
		}
		return sendDataDirect(lun, dataToSend);
	}

	/**
	 * Add an event listener.
	 * 
	 * @param listener
	 */
	public void addListener(DriverEventListener listener) {
		listeners.add(listener);
	}

	/**
	 * Remove an event listener.
	 * 
	 * @param listener
	 */
	public void removeListener(DriverEventListener listener) {
		listeners.remove(listener);
	}

	private static UnsignedInteger lun = null;

	/**
	 * Main method for starting a stand alone test driver using a console
	 * interface.
	 * 
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException,
			InterruptedException {
		TestDriver driver = new TestDriver();
		driver.start();
		System.out.println("Please enter commands:");
		String command = null;
		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(System.in));
		while ((command = bufferedReader.readLine()) != null) {
			String[] commandArgs = command.split(" ");
			try {
				switch (commandArgs[0]) {
				case "lun":
					if (Long.parseLong(commandArgs[1], 16) < 0){
						lun = null;
						break;
					}
					lun = UnsignedInteger.parseUnsignedInteger(commandArgs[1], 16);
					break;
				case "data":

					UnsignedInteger function = UnsignedInteger.parseUnsignedInteger(commandArgs[1], 16);

					byte[][] parameterData = new byte[commandArgs.length - 2][];

					for (int i = 2; i < commandArgs.length; i++) {
						parameterData[i - 2] = HexString
								.toByteArray(commandArgs[i]);
					}
					String response = driver.sendData(lun, function,
							parameterData);

					if (response != null) {
						System.out.println("Response: " + response);
					}

					break;
				case "transmit":

					byte[][] params = new byte[2][];
					
					params[0] = HexString.toByteArray(commandArgs[1]);
					params[1] = new UnsignedInteger(4000).getAsByteArray();

					for (int i = 2; i < commandArgs.length; i++) {
						params[i - 2] = HexString
								.toByteArray(commandArgs[i]);
					}
					response = driver.sendData(lun, NativeDriverInterface.PCSC_FUNCTION_TRANSMIT_TO_ICC,
							params);

					if (response != null) {
						System.out.println("Response: " + response);
					}

					break;
				case "powerup":
					System.out
							.println("Response: "
									+ driver.sendData(
											lun,
											NativeDriverInterface.PCSC_FUNCTION_POWER_ICC,
											new byte[] { 0x01, (byte) 0xf4 }, new UnsignedInteger(4000).getAsByteArray()));
					break;
				case "powerdown":
					System.out
							.println("Response: "
									+ driver.sendData(
											lun,
											NativeDriverInterface.PCSC_FUNCTION_POWER_ICC,
											new byte[] { 0x01, (byte) 0xf5 }, new UnsignedInteger(4000).getAsByteArray()));
					break;
				case "reset":
					System.out
							.println("Response: "
									+ driver.sendData(
											lun,
											NativeDriverInterface.PCSC_FUNCTION_POWER_ICC,
											new byte[] { 0x01, (byte) 0xf6 }, new UnsignedInteger(4000).getAsByteArray()));
					break;
				case "iccpresent":
					System.out
					.println("Response: "
							+ driver.sendData(
									lun,
									NativeDriverInterface.PCSC_FUNCTION_IS_ICC_PRESENT));
			break;
					
				case "exit":
				case "quit":
					driver.stop();
					return;
				default:
					System.out.println("Possible commands:");
					System.out.println("lun <hexNumber>");
					System.out
							.println("data <functionHexNumber> <param1> <param2> ...");
					System.out.println("powerup");
					System.out.println("powerdown");
					System.out.println("iccpresent");
					System.out.println("transmit <payload>");
					System.out.println("reset");
					System.out.println("exit");
					System.out.println("quit");
				}
			} catch (Exception e) {
				System.err
						.println("Error caused by faulty input or severed connection");
			}
		}
	}
}
