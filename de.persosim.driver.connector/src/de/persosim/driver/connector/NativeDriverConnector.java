package de.persosim.driver.connector;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.connector.pcsc.PcscCallResult;
import de.persosim.driver.connector.pcsc.PcscConstants;
import de.persosim.driver.connector.pcsc.PcscDataHelper;
import de.persosim.driver.connector.pcsc.PcscFeature;
import de.persosim.driver.connector.pcsc.PcscListener;
import de.persosim.driver.connector.pcsc.SimplePcscCallResult;
import de.persosim.driver.connector.pcsc.SocketCommunicator;
import de.persosim.driver.connector.pcsc.TlvPcscCallResult;
import de.persosim.simulator.platform.Iso7816;
import de.persosim.simulator.utils.HexString;
import de.persosim.simulator.utils.Utils;

/**
 * This class handles the connection to the native part of the PersoSim driver
 * package via a socket Connection. In PCSC terms this would represent
 * functionality like an IFD Handler presents to the upper layers as described
 * in the PCSC specification part 3.
 * 
 * @author mboonk
 * 
 */
public class NativeDriverConnector implements PcscConstants, PcscListener {

	private static final byte FEATURE_GET_FEATURE_REQUEST = 0;
	private Collection<PcscListener> listeners = new HashSet<PcscListener>();
	private Collection<VirtualReaderUi> userInterfaces = new HashSet<VirtualReaderUi>();
	private NativeDriverComm comm;
	private String nativeDriverHostName;
	private int nativeDriverPort;
	private byte[] cachedAtr = null;
	private Socket simSocket;
	private String simHostName;
	private int simPort;

	public NativeDriverConnector(String nativeDriverHostName,
			int nativeDriverPort, String simHostName, int simPort)
			throws UnknownHostException, IOException {
		this.nativeDriverHostName = nativeDriverHostName;
		this.nativeDriverPort = nativeDriverPort;
		this.simHostName = simHostName;
		this.simPort = simPort;
	}

	/**
	 * This method connects to the native driver part.
	 * 
	 * @throws IOException
	 * @throws UnknownHostException
	 */
	public void connect() throws IOException {
		addListener(this);
		comm = new NativeDriverComm(nativeDriverHostName, nativeDriverPort,
				listeners);
		comm.start();
	}

	/**
	 * This method disconnects from the native driver part.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void disconnect() throws IOException, InterruptedException {
		comm.interrupt();
		comm.join();
	}

	public void addListener(PcscListener listener) {
		listeners.add(listener);
	}

	public void removeListener(PcscListener listener) {
		listeners.remove(listener);
	}

	private void openSimSocket() throws UnknownHostException, IOException {
		if (simSocket == null || simSocket.isClosed()) {
			simSocket = new Socket(simHostName, simPort);
		}
		cachedAtr = CommUtils.exchangeApdu(simSocket,
				HexString.toByteArray("FF010000"));

		for (PcscListener listener : listeners) {
			if (listener instanceof SocketCommunicator) {
				((SocketCommunicator) listener)
						.setCommunicationSocket(simSocket);
			}
		}
	}

	private void closeSimSocket() throws IOException {
		simSocket.close();
		cachedAtr = null;
	}

	public boolean isConnected() {
		return comm.isAlive() && !comm.isInterrupted();
	}

	@Override
	public PcscCallResult processPcscCall(PcscCallData data) {
		switch (data.getFunction()) {
		case NativeDriverComm.PCSC_FUNCTION_DEVICE_LIST_DEVICES:
			return deviceListDevices();
		case NativeDriverComm.PCSC_FUNCTION_DEVICE_CONTROL:
			return deviceControl(data);
		case NativeDriverComm.PCSC_FUNCTION_GET_CAPABILITIES:
			return getCapabilities(data);
		case NativeDriverComm.PCSC_FUNCTION_SET_CAPABILITIES:
			return setCapabilities(data);
		case NativeDriverComm.PCSC_FUNCTION_POWER_ICC:
			return powerIcc(data);
		case NativeDriverComm.PCSC_FUNCTION_TRANSMIT_TO_ICC:
			return transmitToIcc(data);
		}
		return null;
	}

	
	private PcscCallResult deviceListDevices() {
		//logical card slot
		byte [] slot = Utils.concatByteArrays("PersoSim Virtual Reader Slot 1".getBytes(), Utils.toUnsignedByteArray(PcscConstants.DEVICE_TYPE_SLOT));
		
		//pinpad
		byte [] pinpad = Utils.concatByteArrays("PersoSim Virtual Pin Pad".getBytes(), Utils.toUnsignedByteArray(PcscConstants.DEVICE_TYPE_FUNCTIONAL));
		
		return new SimplePcscCallResult(PcscConstants.IFD_SUCCESS, Utils.concatByteArrays(slot, pinpad));
	}

	private PcscCallResult deviceControl(PcscCallData data) {
		// TODO implement
		return null;
	}

	private PcscCallResult getCapabilities(PcscCallData data) {
		// try to find tag in own capabilities

		PcscCallResult result = null;
		byte[] currentTag = data.getParameters().get(0);
		if (Arrays.equals(Utils.toUnsignedByteArray(TAG_VENDOR_NAME),
				currentTag)) {
			result = new TlvPcscCallResult(PcscConstants.IFD_SUCCESS,
					Utils.toUnsignedByteArray(TAG_VENDOR_NAME),
					"HJP Consulting".getBytes(StandardCharsets.US_ASCII));
		} else if (Arrays.equals(Utils.toUnsignedByteArray(TAG_VENDOR_TYPE),
				currentTag)) {
			result = new TlvPcscCallResult(PcscConstants.IFD_SUCCESS,
					Utils.toUnsignedByteArray(TAG_VENDOR_NAME),
					"Virtual Card Reader IFD"
							.getBytes(StandardCharsets.US_ASCII));
		} else if (Arrays.equals(Utils.toUnsignedByteArray(TAG_VENDOR_VERSION),
				currentTag)) {
			result = new TlvPcscCallResult(PcscConstants.IFD_SUCCESS,
					Utils.toUnsignedByteArray(TAG_VENDOR_NAME), new byte[] { 0,
							0, 0, 0 }); // 0xMMmmbbbb MM=major mm=minor
										// bbbb=build
		} else if (Arrays.equals(Utils.toUnsignedByteArray(TAG_VENDOR_SERIAL),
				currentTag)) {
			result = new TlvPcscCallResult(PcscConstants.IFD_SUCCESS,
					Utils.toUnsignedByteArray(TAG_VENDOR_NAME),
					"Serial000000001".getBytes(StandardCharsets.US_ASCII));
		} else if (Arrays.equals(Utils.toUnsignedByteArray(TAG_IFD_ATR),
				currentTag)) {
			if (cachedAtr != null) {
				result = new TlvPcscCallResult(PcscConstants.IFD_SUCCESS,
						Utils.toUnsignedByteArray(TAG_IFD_ATR), cachedAtr);
			}
		} else if (Arrays.equals(
				Utils.toUnsignedByteArray(TAG_IFD_SIMULTANEOUS_ACCESS),
				currentTag)) {
			result = new TlvPcscCallResult(PcscConstants.IFD_SUCCESS,
					Utils.toUnsignedByteArray(TAG_IFD_SIMULTANEOUS_ACCESS),
					new byte[] { 1 });
		} else if (Arrays.equals(
				Utils.toUnsignedByteArray(TAG_IFD_SLOTS_NUMBER), currentTag)) {
			result = new TlvPcscCallResult(PcscConstants.IFD_SUCCESS,
					Utils.toUnsignedByteArray(TAG_IFD_SLOTS_NUMBER),
					new byte[] { 1 });
		} else if (Arrays
				.equals(Utils.toUnsignedByteArray(TAG_IFD_SLOT_THREAD_SAFE),
						currentTag)) {
			result = new TlvPcscCallResult(PcscConstants.IFD_SUCCESS,
					Utils.toUnsignedByteArray(TAG_IFD_SLOT_THREAD_SAFE),
					new byte[] { 0 });
		} else {
			for (PcscListener listener : listeners) {
				if (listener instanceof PcscFeature) {
					PcscFeature feature = (PcscFeature) listener;
					result = new SimplePcscCallResult(
							PcscConstants.IFD_SUCCESS, PcscDataHelper.getField(
									currentTag, feature.getCapabilities()));
					if (result != null) {
						break;
					}
				}
			}
		}

		if (result != null) {
			return result;
		} else {
			return new SimplePcscCallResult(PcscConstants.IFD_ERROR_TAG);
		}
	}

	private PcscCallResult setCapabilities(PcscCallData data) {
		// TODO Auto-generated method stub
		return null;
	}

	private PcscCallResult setProtocolParameters(PcscCallData data) {
		// TODO Auto-generated method stub
		return null;
	}

	private PcscCallResult powerIcc(PcscCallData data) {
		byte[] action = data.getParameters().get(0);
		if (Arrays.equals(Utils.toUnsignedByteArray((short) IFD_POWER_DOWN),
				action)) {
			if (simSocket != null && !simSocket.isClosed()) {
				byte[] result = CommUtils.exchangeApdu(simSocket,
						HexString.toByteArray("FF000000"));
				try {
					closeSimSocket();
					if (Arrays
							.equals(result,
									Utils.toUnsignedByteArray(Iso7816.SW_9000_NO_ERROR))) {
						return new SimplePcscCallResult(IFD_SUCCESS);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return new SimplePcscCallResult(
					PcscConstants.IFD_ERROR_POWER_ACTION);
		} else if (Arrays.equals(
				Utils.toUnsignedByteArray((short) IFD_POWER_UP), action)) {
			try {
				openSimSocket();
				return new TlvPcscCallResult(IFD_SUCCESS,
						Utils.toUnsignedByteArray(TAG_IFD_ATR), cachedAtr);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return new SimplePcscCallResult(
					PcscConstants.IFD_ERROR_POWER_ACTION);
		} else if (Arrays.equals(Utils.toUnsignedByteArray((short) IFD_RESET),
				action)) {
			cachedAtr = CommUtils.exchangeApdu(simSocket,
					HexString.toByteArray("FFFF0000"));
			return new TlvPcscCallResult(IFD_SUCCESS,
					Utils.toUnsignedByteArray(TAG_IFD_ATR), cachedAtr);
		}

		return null;
	}

	private PcscCallResult swallowIcc(PcscCallData data) {
		// TODO Auto-generated method stub
		return null;
	}

	private PcscCallResult ejectIcc(PcscCallData data) {
		// TODO Auto-generated method stub
		return null;
	}

	private PcscCallResult transmitToIcc(PcscCallData data) {
		byte[] inputData = data.getParameters().get(0);
		// ignore the header for now
		// byte [] scardIoHeader = Arrays.copyOfRange(inputData, 0, 8);
		byte[] commandApdu = Arrays.copyOfRange(inputData, 8, inputData.length);
		
		byte[] expectedHeaderAndLc = new byte[] { (byte) 0xff, (byte) 0xc2, 0x01, FEATURE_GET_FEATURE_REQUEST, 0};

		if (Utils.arrayHasPrefix(commandApdu, expectedHeaderAndLc)) {
			return getFeatures();
		}
		
		return new SimplePcscCallResult(PcscConstants.IFD_SUCCESS,
				CommUtils.exchangeApdu(simSocket, commandApdu));
	}

	private PcscCallResult getFeatures() {
		List<Byte> featureDefinitions = new ArrayList<Byte>();
		for (PcscListener listener : listeners){
			if (listener instanceof PcscFeature){
				featureDefinitions.add(((PcscFeature)listener).getFeatureDefinition()[0]);
			}
		}
		byte [] result = new byte [featureDefinitions.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = featureDefinitions.get(i);
		}
		
		return new SimplePcscCallResult(IFD_SUCCESS, result);
	}

	private PcscCallResult isIccPresent(PcscCallData data) {
		// TODO Auto-generated method stub
		return null;
	}

	private PcscCallResult isIccAbsent(PcscCallData data) {
		// TODO Auto-generated method stub
		return null;
	}

	private PcscCallResult listContexts(PcscCallData data) {
		// TODO Auto-generated method stub
		return null;
	}

	private PcscCallResult isContextSupported(PcscCallData data) {
		// TODO Auto-generated method stub
		return null;
	}

	private PcscCallResult getIfdsp(PcscCallData data) {
		// TODO Auto-generated method stub
		return null;
	}

	private PcscCallResult listInterfaces(PcscCallData data) {
		// TODO Auto-generated method stub
		return null;
	}

	public void addUi(VirtualReaderUi ui) {
		this.userInterfaces.add(ui);
	}
	
	public void removeUi(VirtualReaderUi ui){
		this.userInterfaces.remove(ui);
	}
}
