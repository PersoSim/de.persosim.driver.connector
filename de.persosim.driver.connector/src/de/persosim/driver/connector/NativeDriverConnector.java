package de.persosim.driver.connector;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.connector.pcsc.PcscCallResult;
import de.persosim.driver.connector.pcsc.PcscFeature;
import de.persosim.driver.connector.pcsc.PcscListener;
import de.persosim.simulator.utils.HexString;

/**
 * This class handles the connection to the native part of the PersoSim driver
 * package via a socket Connection. In PCSC terms this would represent
 * functionality like an IFD Handler presents to the upper layers as described
 * in the PCSC specification part 3.
 * 
 * @author mboonk
 * 
 */
public class NativeDriverConnector implements PcscListener, PcscCommonMethods, PcscSlotLogicalDeviceMethods{

	Collection<PcscListener> listeners = new HashSet<PcscListener>();
	private NativeDriverComm comm;
	private String hostName;
	private int dataPort;

	public NativeDriverConnector(String hostName, int dataPort) throws UnknownHostException, IOException {
		this.hostName = hostName;
		this.dataPort = dataPort;
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
		comm = new NativeDriverComm(hostName, dataPort, listeners);
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
		byte [] TAG_VENDOR_NAME = new byte [] {0,0,0x01, 0x00};
		byte [] TAG_VENDOR_TYPE = new byte [] {0,0,0x01, 0x01};
		byte [] TAG_VENDOR_VERSION = new byte [] {0,0,0x01, 0x02};
		byte [] TAG_VENDOR_SERIAL = new byte [] {0,0,0x01, 0x03};
		
		byte [] result = null;
		byte [] currentTag = data.getParameters().get(0);
		if (Arrays.equals(TAG_VENDOR_NAME, currentTag)){
			result = PcscDataHelper.buildTlv(TAG_VENDOR_NAME, "HJP Consulting".getBytes(StandardCharsets.US_ASCII));
		} else if (Arrays.equals(TAG_VENDOR_TYPE, currentTag)){
			result = PcscDataHelper.buildTlv(TAG_VENDOR_NAME, "Virtual Card Reader IFD".getBytes(StandardCharsets.US_ASCII));
		} else if (Arrays.equals(TAG_VENDOR_VERSION, currentTag)){
			result = PcscDataHelper.buildTlv(TAG_VENDOR_NAME, new byte []{0,0,0,0}); //0xMMmmbbbb MM=major mm=minor bbbb=build
		} else if (Arrays.equals(TAG_VENDOR_SERIAL, currentTag)){
			result = PcscDataHelper.buildTlv(TAG_VENDOR_NAME, "Serial000000001".getBytes(StandardCharsets.US_ASCII));
		} else {
			for (PcscListener listener : listeners){
				if (listener instanceof PcscFeature){
					PcscFeature feature = (PcscFeature) listener;
					result = PcscDataHelper.getFieldContent(currentTag, feature.getCapabilities());
					if (result != null){
						break;
					}
				}
			}
		}
		
		if (result != null){
			final String resultData = HexString.encode(result);
			return new PcscCallResult() {
				
				@Override
				public String getEncoded() {
					return PcscConstants.IFD_SUCCESS + "#" + resultData;
				}
			};
		}
		return null;
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
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		return null;
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
