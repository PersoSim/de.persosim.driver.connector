package de.persosim.driver.connector;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.connector.pcsc.PcscCallResult;
import de.persosim.driver.connector.pcsc.PcscCommonMethods;
import de.persosim.driver.connector.pcsc.PcscCommunicationServices;
import de.persosim.driver.connector.pcsc.PcscConstants;
import de.persosim.driver.connector.pcsc.PcscDataHelper;
import de.persosim.driver.connector.pcsc.PcscFeature;
import de.persosim.driver.connector.pcsc.PcscListener;
import de.persosim.driver.connector.pcsc.PcscSlotLogicalDeviceMethods;
import de.persosim.driver.connector.pcsc.SimplePcscCallResult;
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
public class NativeDriverConnector implements PcscConstants, PcscListener, PcscCommonMethods, PcscSlotLogicalDeviceMethods, PcscCommunicationServices{

	Collection<PcscListener> listeners = new HashSet<PcscListener>();
	private NativeDriverComm comm;
	private String nativeDriverHostName;
	private int nativeDriverPort;
	private byte [] cachedAtr = null;
	private Socket simSocket;
	private String simHostName;
	private int simPort;

	public NativeDriverConnector(String nativeDriverHostName, int nativeDriverPort, String simHostName, int simPort) throws UnknownHostException, IOException {
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
		addListener(new PcscPrinter());
		addListener(this);
		comm = new NativeDriverComm(nativeDriverHostName, nativeDriverPort, listeners);
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

	public boolean isConnected() {
		return comm.isAlive() && !comm.isInterrupted();
	}
	
	@Override
	public PcscCallResult processPcscCall(PcscCallData data) {
		switch(data.getFunction()){
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

	@Override
	public PcscCallResult deviceListDevices() {
		//TODO implement
		return null;
	}

	@Override
	public PcscCallResult deviceControl(PcscCallData data) {
		//TODO implement
		return null;
	}

	@Override
	public PcscCallResult getCapabilities(PcscCallData data) {
		//try to find tag in own capabilities
		
		PcscCallResult result = null;
		byte [] currentTag = data.getParameters().get(0);
		if (Arrays.equals(Utils.toUnsignedByteArray(TAG_VENDOR_NAME), currentTag)){
			result = new TlvPcscCallResult(PcscConstants.IFD_SUCCESS, Utils.toUnsignedByteArray(TAG_VENDOR_NAME), "HJP Consulting".getBytes(StandardCharsets.US_ASCII));
		} else if (Arrays.equals(Utils.toUnsignedByteArray(TAG_VENDOR_TYPE), currentTag)){
			result = new TlvPcscCallResult(PcscConstants.IFD_SUCCESS, Utils.toUnsignedByteArray(TAG_VENDOR_NAME), "Virtual Card Reader IFD".getBytes(StandardCharsets.US_ASCII));
		} else if (Arrays.equals(Utils.toUnsignedByteArray(TAG_VENDOR_VERSION), currentTag)){
			result = new TlvPcscCallResult(PcscConstants.IFD_SUCCESS, Utils.toUnsignedByteArray(TAG_VENDOR_NAME), new byte []{0,0,0,0}); //0xMMmmbbbb MM=major mm=minor bbbb=build
		} else if (Arrays.equals(Utils.toUnsignedByteArray(TAG_VENDOR_SERIAL), currentTag)){
			result = new TlvPcscCallResult(PcscConstants.IFD_SUCCESS, Utils.toUnsignedByteArray(TAG_VENDOR_NAME), "Serial000000001".getBytes(StandardCharsets.US_ASCII));
		} else if (Arrays.equals(Utils.toUnsignedByteArray(TAG_IFD_ATR), currentTag)){
			if (cachedAtr != null){
				result = new TlvPcscCallResult(PcscConstants.IFD_SUCCESS, Utils.toUnsignedByteArray(TAG_IFD_ATR), cachedAtr);
			}
		} else if (Arrays.equals(Utils.toUnsignedByteArray(TAG_IFD_SIMULTANEOUS_ACCESS), currentTag)){
			result = new TlvPcscCallResult(PcscConstants.IFD_SUCCESS, Utils.toUnsignedByteArray(TAG_IFD_SIMULTANEOUS_ACCESS), new byte [] {1});
		} else if (Arrays.equals(Utils.toUnsignedByteArray(TAG_IFD_SLOTS_NUMBER), currentTag)){
			result = new TlvPcscCallResult(PcscConstants.IFD_SUCCESS, Utils.toUnsignedByteArray(TAG_IFD_SLOTS_NUMBER), new byte [] {1});
		} else if (Arrays.equals(Utils.toUnsignedByteArray(TAG_IFD_SLOT_THREAD_SAFE), currentTag)){
			result = new TlvPcscCallResult(PcscConstants.IFD_SUCCESS, Utils.toUnsignedByteArray(TAG_IFD_SLOT_THREAD_SAFE), new byte [] {0});
		} else {
			for (PcscListener listener : listeners){
				if (listener instanceof PcscFeature){
					PcscFeature feature = (PcscFeature) listener;
					result = new SimplePcscCallResult(PcscConstants.IFD_SUCCESS, PcscDataHelper.getField(currentTag, feature.getCapabilities()));
					if (result != null){
						break;
					}
				}
			}
		}
		
		if (result != null){
			return result;
		} else {
			return new SimplePcscCallResult(PcscConstants.IFD_ERROR_TAG);
		}
	}

	@Override
	public PcscCallResult setCapabilities(PcscCallData data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PcscCallResult setProtocolParameters(PcscCallData data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PcscCallResult powerIcc(PcscCallData data) {
		byte [] action = data.getParameters().get(0);
		if (Arrays.equals(Utils.toUnsignedByteArray((short)IFD_POWER_DOWN), action)){
			if(simSocket != null && !simSocket.isClosed()){
				byte [] result = CommUtils.exchangeApdu(simSocket, HexString.toByteArray("FF000000"));
				try {
					simSocket.close();
					cachedAtr = null;
					if (Arrays.equals(result, Utils.toUnsignedByteArray(Iso7816.SW_9000_NO_ERROR))){
						return new SimplePcscCallResult(IFD_SUCCESS);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return new SimplePcscCallResult(PcscConstants.IFD_ERROR_POWER_ACTION);
		} else if (Arrays.equals(Utils.toUnsignedByteArray((short)IFD_POWER_UP), action)){
			try {
				if (simSocket == null || simSocket.isClosed()){
					simSocket = new Socket(simHostName, simPort);
				}
				cachedAtr = CommUtils.exchangeApdu(simSocket, HexString.toByteArray("FF010000"));
				return new TlvPcscCallResult(IFD_SUCCESS, Utils.toUnsignedByteArray(TAG_IFD_ATR), cachedAtr);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return new SimplePcscCallResult(PcscConstants.IFD_ERROR_POWER_ACTION);
		} else if (Arrays.equals(Utils.toUnsignedByteArray((short)IFD_RESET), action)){
			cachedAtr = CommUtils.exchangeApdu(simSocket, HexString.toByteArray("FFFF0000"));
			return new TlvPcscCallResult(IFD_SUCCESS, Utils.toUnsignedByteArray(TAG_IFD_ATR), cachedAtr);
		}
		
		return null;
	}

	@Override
	public PcscCallResult swallowIcc(PcscCallData data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PcscCallResult ejectIcc(PcscCallData data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PcscCallResult transmitToIcc(PcscCallData data) {
		byte [] inputData = data.getParameters().get(0);
		//ignore the header for now
		//byte [] scardIoHeader = Arrays.copyOfRange(inputData, 0, 8);
		byte [] commandApdu = Arrays.copyOfRange(inputData, 8, inputData.length);
		return new SimplePcscCallResult(PcscConstants.IFD_SUCCESS, CommUtils.exchangeApdu(simSocket, commandApdu));
	}

	@Override
	public PcscCallResult isIccPresent(PcscCallData data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PcscCallResult isIccAbsent(PcscCallData data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PcscCallResult listContexts(PcscCallData data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PcscCallResult isContextSupported(PcscCallData data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PcscCallResult getIfdsp(PcscCallData data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PcscCallResult listInterfaces(PcscCallData data) {
		// TODO Auto-generated method stub
		return null;
	}
}
