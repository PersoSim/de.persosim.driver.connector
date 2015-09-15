package de.persosim.driver.connector;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.connector.pcsc.PcscCallResult;
import de.persosim.driver.connector.pcsc.PcscConstants;
import de.persosim.driver.connector.pcsc.PcscFeature;
import de.persosim.driver.connector.pcsc.PcscListener;
import de.persosim.driver.connector.pcsc.SimplePcscCallResult;
import de.persosim.driver.connector.pcsc.UiEnabled;
import de.persosim.driver.connector.service.NativeDriverConnectorInterface;
import de.persosim.simulator.platform.Iso7816;
import de.persosim.simulator.utils.PersoSimLogger;
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
public class NativeDriverConnector implements PcscConstants, PcscListener, NativeDriverConnectorInterface {
	
	public static final String DEFAULT_HOST = "localhost";
	public static final int DEFAULT_PORT = 5678;

	private static final byte FEATURE_GET_FEATURE_REQUEST = 0;
	private List<PcscListener> listeners = new ArrayList<PcscListener>();
	private List<VirtualReaderUi> userInterfaces = new ArrayList<VirtualReaderUi>();
	private Thread communicationThread;
	private NativeDriverComm communication;
	
	private String nativeDriverHostName = null;
	private int nativeDriverPort = Integer.MIN_VALUE;
	private byte[] cachedAtr = null;
	private int timeout = 5000;

	/**
	 * Create a connector object
	 */
	public NativeDriverConnector() {
		listeners.add(this);
	}

	/* (non-Javadoc)
	 * @see de.persosim.driver.connector.XEX#connect(java.lang.String, int)
	 */
	@Override
	public void connect(String nativeDriverHostName, int nativeDriverPort) throws IOException {
		this.nativeDriverHostName = nativeDriverHostName;
		this.nativeDriverPort = nativeDriverPort;
		
		communication = new NativeDriverComm(this.nativeDriverHostName, this.nativeDriverPort, listeners); 
		communicationThread = new Thread(communication);
		communicationThread.start();

		long timeOutTime = Calendar.getInstance().getTimeInMillis() + timeout;
		while (!communication.isConnected()){
			if (Calendar.getInstance().getTimeInMillis() > timeOutTime){
				throw new IOException("The communication thread has run into a timeout");
			}
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				throw new IOException("The waiting for the communication thread was interrupted");
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see de.persosim.driver.connector.XEX#isRunning()
	 */
	@Override
	public boolean isRunning() {
		return ((communicationThread != null) && communicationThread.isAlive());
	}

	/* (non-Javadoc)
	 * @see de.persosim.driver.connector.XEX#disconnect()
	 */
	@Override
	public void disconnect() throws IOException, InterruptedException {		
		communication.disconnect();
		communicationThread.join();
	}

	@Override
	public void addListener(PcscListener listener) {
		//add the new listener behind the others but preserve the position of this object
		listeners.add(listeners.size() - 1, listener);
		if (listener instanceof UiEnabled){
			((UiEnabled)listener).setUserInterfaces(userInterfaces);
		}
	}

	@Override
	public void removeListener(PcscListener listener) {
		listeners.remove(listener);
	}

	/* (non-Javadoc)
	 * @see de.persosim.driver.connector.XEX#addUi(de.persosim.driver.connector.VirtualReaderUi)
	 */
	@Override
	public void addUi(VirtualReaderUi ui) {
		this.userInterfaces.add(ui);
	}

	/* (non-Javadoc)
	 * @see de.persosim.driver.connector.XEX#removeUi(de.persosim.driver.connector.VirtualReaderUi)
	 */
	@Override
	public void removeUi(VirtualReaderUi ui) {
		this.userInterfaces.remove(ui);
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
		UnsignedInteger expectedLength = CommUtils.getExpectedLength(data, 2);

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
		UnsignedInteger expectedLength = CommUtils.getExpectedLength(data, 1);

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
		UnsignedInteger expectedLength = CommUtils.getExpectedLength(data, 1);

		if (expectedLength == null){
			return new SimplePcscCallResult(PcscConstants.IFD_ERROR_INSUFFICIENT_BUFFER);
		}
		
		if (Activator.getSim() == null){
			PersoSimLogger.log(getClass(), "The simulator service is not available", PersoSimLogger.WARN);
			return new SimplePcscCallResult(PcscConstants.IFD_ERROR_POWER_ACTION);
		}
				
		if (IFD_POWER_DOWN.equals(
				action)) {
			if (Activator.getSim().isRunning()) {
				byte[] result;
				
				result = Activator.getSim().cardPowerDown();
				
				if (Arrays.equals(result,
						Utils.toUnsignedByteArray(Iso7816.SW_9000_NO_ERROR))) {
					return new SimplePcscCallResult(IFD_SUCCESS);
				}
				return new SimplePcscCallResult(PcscConstants.IFD_ERROR_POWER_ACTION);
			}
			return new SimplePcscCallResult(IFD_SUCCESS); //already powered down
		} else {
			if (IFD_POWER_UP.equals(action)) {
				if (Activator.getSim().isRunning())
					cachedAtr = Activator.getSim().cardPowerUp();
				else 
					PersoSimLogger.log(getClass(), "The simulator is not running, card was not powered up", PersoSimLogger.WARN);
			} else if (IFD_RESET.equals(
					action)) {
				
				cachedAtr = Activator.getSim().cardReset();
			}
			
			if (cachedAtr == null) {
				return new SimplePcscCallResult(PcscConstants.IFD_ERROR_POWER_ACTION);
			}
			
			if (cachedAtr != null && cachedAtr.length <= expectedLength.getAsSignedLong()){
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
		UnsignedInteger expectedLength = CommUtils.getExpectedLength(data, 1);

		if (expectedLength == null){
			return new SimplePcscCallResult(PcscConstants.IFD_ERROR_INSUFFICIENT_BUFFER);
		}
		
		byte[] expectedHeaderAndLc = new byte[] { (byte) 0xff, (byte) 0xc2,
				0x01, FEATURE_GET_FEATURE_REQUEST, 0 };

		byte [] result = new byte [0];
		
		if (Utils.arrayHasPrefix(commandApdu, expectedHeaderAndLc)) {
			result = getOnlyTagsFromFeatureList(getFeatures());
		} else {
			result = Activator.getSim().processCommand(commandApdu);
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
		if ((Activator.getSim() != null)&& (Activator.getSim().isRunning())){
			return new SimplePcscCallResult(IFD_ICC_PRESENT);
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
