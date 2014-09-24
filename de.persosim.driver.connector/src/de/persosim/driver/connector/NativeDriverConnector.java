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
	private Collection<PcscListener> listeners = new ArrayList<PcscListener>();
	private Collection<VirtualReaderUi> userInterfaces = new HashSet<VirtualReaderUi>();
	private NativeDriverComm comm;
	private String nativeDriverHostName;
	private int nativeDriverPort;
	private byte[] cachedAtr = null;
	private Socket simSocket;
	private String simHostName;
	private int simPort;

	/**
	 * Create a connector object using the connection data for the native driver
	 * part and the simulation.
	 * 
	 * @param nativeDriverHostName
	 * @param nativeDriverPort
	 * @param simHostName
	 * @param simPort
	 * @throws UnknownHostException
	 * @throws IOException
	 */
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

	/**
	 * Add a listener that is to be informed about PCSC calls.
	 * 
	 * @param listener
	 */
	public void addListener(PcscListener listener) {
		listeners.add(listener);
	}

	/**
	 * Remove a listener and stop informing it about PCSC calls.
	 * 
	 * @param listener
	 */
	public void removeListener(PcscListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Add an additional user interface.
	 * 
	 * @param ui
	 */
	public void addUi(VirtualReaderUi ui) {
		this.userInterfaces.add(ui);
	}

	/**
	 * Remove a user interface.
	 * 
	 * @param ui
	 */
	public void removeUi(VirtualReaderUi ui) {
		this.userInterfaces.remove(ui);
	}

	private void powerUp() throws UnknownHostException, IOException {
		openSimSocket();
		cachedAtr = CommUtils.exchangeApdu(simSocket,
				HexString.toByteArray("FF010000"));

		for (PcscListener listener : listeners) {
			if (listener instanceof SocketCommunicator) {
				((SocketCommunicator) listener)
						.setCommunicationSocket(simSocket);
			}
		}
	}

	private void openSimSocket() throws UnknownHostException, IOException {
		if (simSocket == null || simSocket.isClosed()) {
			simSocket = new Socket(simHostName, simPort);
		}
	}

	private void closeSimSocket() throws IOException {
		simSocket.close();
		cachedAtr = null;
	}

	/**
	 * @return true, iff the communication thread is alive and not interrupted
	 */
	public boolean isConnected() {
		return comm.isAlive() && !comm.isInterrupted();
	}
	
	private UnsignedInteger getExpectedLength(PcscCallData data, int expectedLengthParameterIndex){
		if (expectedLengthParameterIndex < data.getParameters().size()){
			return new UnsignedInteger(data.getParameters().get(expectedLengthParameterIndex));
		}
		return null;
	}

	@Override
	public PcscCallResult processPcscCall(PcscCallData data) {
		switch (data.getFunction().getAsInt()) {
		case NativeDriverInterface.VALUE_PCSC_FUNCTION_DEVICE_LIST_DEVICES:
			return deviceListDevices();
		case NativeDriverInterface.VALUE_PCSC_FUNCTION_DEVICE_CONTROL:
			return deviceControl(data);
		case NativeDriverInterface.VALUE_PCSC_FUNCTION_GET_CAPABILITIES:
			return getCapabilities(data);
		case NativeDriverInterface.VALUE_PCSC_FUNCTION_SET_CAPABILITIES:
			return setCapabilities(data);
		case NativeDriverInterface.VALUE_PCSC_FUNCTION_POWER_ICC:
			return powerIcc(data);
		case NativeDriverInterface.VALUE_PCSC_FUNCTION_TRANSMIT_TO_ICC:
			return transmitToIcc(data);
		case NativeDriverInterface.VALUE_PCSC_FUNCTION_IS_ICC_PRESENT:
			return isIccPresent(data);
		case NativeDriverInterface.VALUE_PCSC_FUNCTION_EJECT_ICC:
			return ejectIcc(data);
		case NativeDriverInterface.VALUE_PCSC_FUNCTION_GET_IFDSP:
			return getIfdsp(data);
		case NativeDriverInterface.VALUE_PCSC_FUNCTION_IS_CONTEXT_SUPPORTED:
			return isContextSupported(data);
		case NativeDriverInterface.VALUE_PCSC_FUNCTION_IS_ICC_ABSENT:
			return isIccAbsent(data);
		case NativeDriverInterface.VALUE_PCSC_FUNCTION_LIST_CONTEXTS:
			return listContexts(data);
		case NativeDriverInterface.VALUE_PCSC_FUNCTION_LIST_INTERFACES:
			return listInterfaces(data);
		case NativeDriverInterface.VALUE_PCSC_FUNCTION_SET_PROTOCOL_PARAMETERS:
			return setProtocolParameters(data);
		case NativeDriverInterface.VALUE_PCSC_FUNCTION_SWALLOW_ICC:
			return swallowIcc(data);
		}
		return null;
	}

	private PcscCallResult deviceListDevices() {
		// logical card slot
		byte[] result = Utils.concatByteArrays(CommUtils
				.getNullTerminatedAsciiString("PersoSim Virtual Reader Slot"),
				PcscConstants.DEVICE_TYPE_SLOT.getAsByteArray());

		for (VirtualReaderUi ui : userInterfaces) {
			result = Utils.concatByteArrays(result, ui.getDeviceDescriptors());
		}

		return new SimplePcscCallResult(PcscConstants.IFD_SUCCESS, result);
	}

	private PcscCallResult deviceControl(PcscCallData data) {
		UnsignedInteger controlCode = new UnsignedInteger(data.getParameters().get(0));
		UnsignedInteger expectedLength = getExpectedLength(data, 2);

		if (expectedLength == null){
			return new SimplePcscCallResult(PcscConstants.IFD_ERROR_INSUFFICIENT_BUFFER);
		}
		
		if (controlCode.equals(PcscConstants.CONTROL_CODE_GET_FEATURE_REQUEST)) {
			List<byte[]> features = getFeatures();
			byte[] resultData = new byte[0];
			for (int i = 0; i < features.size(); i++) {
				resultData = Utils
						.concatByteArrays(resultData, features.get(i));
			}
			if (resultData.length > expectedLength.getAsSignedLong()){
				return new SimplePcscCallResult(IFD_ERROR_INSUFFICIENT_BUFFER);
			}
			return new SimplePcscCallResult(IFD_SUCCESS, resultData);
		}
		return null;
	}

	private PcscCallResult getCapabilities(PcscCallData data) {
		// try to find tag in own capabilities
		byte [] result = null;
		UnsignedInteger currentTag = new UnsignedInteger(data.getParameters().get(0));
		UnsignedInteger expectedLength = getExpectedLength(data, 1);

		if (expectedLength == null){
			return new SimplePcscCallResult(PcscConstants.IFD_ERROR_INSUFFICIENT_BUFFER);
		}
		
		if (TAG_VENDOR_NAME.equals(currentTag)) {
			result = "HJP Consulting".getBytes(StandardCharsets.US_ASCII);
		} else if (TAG_VENDOR_TYPE.equals(
				currentTag)) {
			result = "Virtual Card Reader IFD"
							.getBytes(StandardCharsets.US_ASCII);
		} else if (TAG_VENDOR_VERSION.equals(
				currentTag)) {
			result = new byte[] { 0,
							0, 0, 0 }; // 0xMMmmbbbb MM=major mm=minor
										// bbbb=build
		} else if (TAG_VENDOR_SERIAL.equals(
				currentTag)) {
			result = "Serial000000001".getBytes(StandardCharsets.US_ASCII);
		} else if (TAG_IFD_ATR.equals(
				currentTag)) {
			if (cachedAtr != null) {
				result = cachedAtr;
			}
		} else if (TAG_IFD_SIMULTANEOUS_ACCESS.equals(
				currentTag)) {
			result = 					new byte[] { 1 };
		} else if (TAG_IFD_SLOTS_NUMBER.equals( currentTag)) {
			result = 
					new byte[] { 1 };
		} else if (TAG_IFD_SLOT_THREAD_SAFE.equals(
						currentTag)) {
			result = 
					new byte[] { 0 };
		} else {
			for (PcscListener listener : listeners) {
				if (listener instanceof PcscFeature) {
					PcscFeature feature = (PcscFeature) listener;
					byte [] field = PcscDataHelper.getField(
							currentTag, feature.getCapabilities());
					if (field != null){
						result = field;
						break;
					}
				}
			}
		}

		if (result != null) {
			if (result.length > expectedLength.getAsSignedLong()){
				return new SimplePcscCallResult(PcscConstants.IFD_ERROR_INSUFFICIENT_BUFFER);
			}
			return new SimplePcscCallResult(PcscConstants.IFD_SUCCESS, result);
		} 
		return new SimplePcscCallResult(PcscConstants.IFD_ERROR_TAG);
	}

	private PcscCallResult setCapabilities(PcscCallData data) {
		// TODO Implement
		return null;
	}

	private PcscCallResult setProtocolParameters(PcscCallData data) {
		// TODO Implement
		return null;
	}

	private PcscCallResult powerIcc(PcscCallData data) {
		UnsignedInteger action = new UnsignedInteger(data.getParameters().get(0));
		UnsignedInteger expectedLength = getExpectedLength(data, 1);

		if (expectedLength == null){
			return new SimplePcscCallResult(PcscConstants.IFD_ERROR_INSUFFICIENT_BUFFER);
		}
		
		if (IFD_POWER_DOWN.equals(
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
		} else {
			if (IFD_POWER_UP.equals(action)) {
				try {
					powerUp();
				} catch (IOException e) {
					return new SimplePcscCallResult(
							PcscConstants.IFD_ERROR_POWER_ACTION);
				}
			} else if (IFD_RESET.equals(
					action)) {
				cachedAtr = CommUtils.exchangeApdu(simSocket,
						HexString.toByteArray("FFFF0000"));
			}
			if (cachedAtr.length <= expectedLength.getAsSignedLong()){
				return new SimplePcscCallResult(IFD_SUCCESS, cachedAtr);	
			}
			return new SimplePcscCallResult(IFD_ERROR_INSUFFICIENT_BUFFER);
		}
	}

	private PcscCallResult swallowIcc(PcscCallData data) {
		// TODO Implement
		return null;
	}

	private PcscCallResult ejectIcc(PcscCallData data) {
		// TODO Implement
		return null;
	}

	private PcscCallResult transmitToIcc(PcscCallData data) {
		byte[] commandApdu = data.getParameters().get(0);
		UnsignedInteger expectedLength = getExpectedLength(data, 1);

		if (expectedLength == null){
			return new SimplePcscCallResult(PcscConstants.IFD_ERROR_INSUFFICIENT_BUFFER);
		}
		
		byte[] expectedHeaderAndLc = new byte[] { (byte) 0xff, (byte) 0xc2,
				0x01, FEATURE_GET_FEATURE_REQUEST, 0 };

		byte [] result = new byte [0];
		
		if (Utils.arrayHasPrefix(commandApdu, expectedHeaderAndLc)) {
			result = getOnlyTagsFromFeatureList(getFeatures());
		} else {
			result = CommUtils.exchangeApdu(simSocket, commandApdu);
		}

		if (result.length > expectedLength.getAsSignedLong()){
			return new SimplePcscCallResult(PcscConstants.IFD_ERROR_INSUFFICIENT_BUFFER);
		}
		
		return new SimplePcscCallResult(PcscConstants.IFD_SUCCESS,
				result);
		
	}

	private byte[] getOnlyTagsFromFeatureList(List<byte[]> features) {
		byte[] result = new byte[features.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = features.get(i)[0];
		}
		return result;
	}

	private List<byte[]> getFeatures() {
		List<byte[]> featureDefinitions = new ArrayList<byte[]>();
		for (PcscListener listener : listeners) {
			if (listener instanceof PcscFeature) {
				featureDefinitions.add(((PcscFeature) listener)
						.getFeatureDefinition());
			}
		}

		return featureDefinitions;
	}

	private PcscCallResult isIccPresent(PcscCallData data) {
		boolean closeConnection = false;
		try {
			if (simSocket == null || simSocket.isClosed()){
				openSimSocket();	
				closeConnection = true;
			}

			byte[] response = CommUtils.exchangeApdu(simSocket, new byte[] {
					(byte) 0xff, (byte) 0x90, 0, 0 });

			if (Arrays.equals(response,
					Utils.toUnsignedByteArray(Iso7816.SW_9000_NO_ERROR))) {
				return new SimplePcscCallResult(IFD_ICC_PRESENT);
			}
		} catch (IOException e) {
			System.out.println("The socket communication with " + simHostName
					+ ":" + simPort + " could not be established");
		} finally {
			if (closeConnection){
				try {
					closeSimSocket();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return new SimplePcscCallResult(IFD_ICC_NOT_PRESENT);
	}

	private PcscCallResult isIccAbsent(PcscCallData data) {
		// TODO Implement
		return null;
	}

	private PcscCallResult listContexts(PcscCallData data) {
		// TODO Implement
		return null;
	}

	private PcscCallResult isContextSupported(PcscCallData data) {
		// TODO Implement
		return null;
	}

	private PcscCallResult getIfdsp(PcscCallData data) {
		// TODO Implement
		return null;
	}

	private PcscCallResult listInterfaces(PcscCallData data) {
		// TODO Implement
		return null;
	}
}
