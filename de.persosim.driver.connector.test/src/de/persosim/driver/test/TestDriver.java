package de.persosim.driver.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import de.persosim.driver.connector.NativeDriverInterface;
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

	private TestDriverCommunicationThread communicationThread;
	private HashMap<Integer, Socket> lunMapping = new HashMap<>();

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
			communicationThread = new TestDriverCommunicationThread(port,
					lunMapping, listeners);
			communicationThread.setDaemon(true);
			communicationThread.start();

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
			running = false;
		} else {
			System.out.println("This test driver instance is not running");
		}
	}

	/**
	 * Send data from the test driver to simulate PCSC calls.
	 * 
	 * @param lun
	 * @param function
	 * @param data
	 * @return the resulting answer to the PCSC call
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public String sendData(int lun, int function, byte[]... data)
			throws IOException, InterruptedException {
		Socket iccSocket = lunMapping.get(lun);
		if (iccSocket != null && iccSocket.isConnected()) {
			System.out.println("Driver sending data to connector");
			BufferedWriter bufferedOut = new BufferedWriter(
					new OutputStreamWriter(iccSocket.getOutputStream()));
			String dataToSend = function + NativeDriverInterface.MESSAGE_DIVIDER + lun;

			for (byte[] current : data) {
				dataToSend += NativeDriverInterface.MESSAGE_DIVIDER + HexString.encode(current);
			}

			bufferedOut.write(dataToSend);
			bufferedOut.newLine();
			bufferedOut.flush();

			BufferedReader bufferedIn = new BufferedReader(
					new InputStreamReader(iccSocket.getInputStream()));
			System.out.println("Driver waiting for response from connector");

			String responseData = bufferedIn.readLine();
			for (DriverEventListener listener : listeners) {
				System.out.println("Notifing listener of type "
						+ listener.getClass().getSimpleName());
				listener.messageReceivedCallback(responseData);
			}
			return responseData;
		}
		System.out.println("No connected socket found for lun: " + lun);
		return null;
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

	private static int lun = -1;

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
					lun = Integer.parseInt(commandArgs[1]);
					break;
				case "data":

					int function = Integer.parseInt(commandArgs[1]);

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
				case "powerup":
					System.out
							.println("Response: "
									+ driver.sendData(
											lun,
											NativeDriverInterface.PCSC_FUNCTION_POWER_ICC,
											new byte[] { 0x01, (byte) 0xf4 }));
					break;
				case "powerdown":
					System.out
							.println("Response: "
									+ driver.sendData(
											lun,
											NativeDriverInterface.PCSC_FUNCTION_POWER_ICC,
											new byte[] { 0x01, (byte) 0xf5 }));
					break;
				case "reset":
					System.out
							.println("Response: "
									+ driver.sendData(
											lun,
											NativeDriverInterface.PCSC_FUNCTION_POWER_ICC,
											new byte[] { 0x01, (byte) 0xf6 }));
					break;
				case "exit":
				case "quit":
					driver.stop();
					return;
				default:
					System.out.println("Possible commands:");
					System.out.println("lun <number>");
					System.out
							.println("data <functionNumber> <param1> <param2> ...");
					System.out.println("powerup");
					System.out.println("powerdown");
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
