package de.persosim.driver.connector.features;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import de.persosim.driver.connector.NativeDriverInterface;
import de.persosim.driver.connector.UnsignedInteger;
import de.persosim.driver.connector.VirtualReaderUi;
import de.persosim.driver.connector.pcsc.AbstractPcscFeature;
import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.connector.pcsc.PcscCallResult;
import de.persosim.driver.connector.pcsc.PcscConstants;
import de.persosim.driver.connector.pcsc.SimplePcscCallResult;
import de.persosim.driver.connector.pcsc.UiEnabled;
import de.persosim.simulator.utils.HexString;
import de.persosim.simulator.utils.Utils;

public class ModifyPinDirect extends AbstractPcscFeature implements UiEnabled {

	public static final byte FEATURE_TAG = 0x07;
	
	private Collection<VirtualReaderUi> interfaces;
	
	public ModifyPinDirect(UnsignedInteger controlCode) {
		super(controlCode, FEATURE_TAG);
		// TODO Auto-generated constructor stub
	}

	@Override
	public byte[] getCapabilities() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PcscCallResult processPcscCall(PcscCallData data) {
		if (data.getFunction().equals(NativeDriverInterface.PCSC_FUNCTION_DEVICE_CONTROL)){
			if (Arrays.equals(data.getParameters().get(0), getControlCode().getAsByteArray())){
				List<byte[]> params = data.getParameters();
				byte[] param = params.get(1);
				
				// XXX SLS actually use data from param
				
				byte[] resetRetryCounterApduBytes;
				
				byte[] newPin1, newPin2;
				boolean match;
				
				for(VirtualReaderUi virtualReaderUi : interfaces) {
					match = false;
					
					while(!match) {
						virtualReaderUi.display("Please enter new PIN");
						try {
							newPin1 = virtualReaderUi.getPin();
						} catch (IOException e) {
							newPin1 = null;
						}
						
						if(newPin1 == null) {
							return new SimplePcscCallResult(PcscConstants.IFD_COMMUNICATION_ERROR, new byte[0]);
						}
						
						virtualReaderUi.display("Please re-enter new PIN");
						try {
							newPin2 = virtualReaderUi.getPin();
						} catch (IOException e) {
							newPin2 = null;
						}
						
						if(newPin2 == null) {
							return new SimplePcscCallResult(PcscConstants.IFD_COMMUNICATION_ERROR, new byte[0]);
						}
						
						match = Arrays.equals(newPin1, newPin2) && (newPin1 != null);
						
						if(match) {
							resetRetryCounterApduBytes = Utils.concatByteArrays(param, HexString.toByteArray(HexString.hexifyByte(newPin1.length)), newPin1);
							params.set(1, resetRetryCounterApduBytes);
							System.out.println("end: " + HexString.encode(params.get(1)));
							System.out.println("modified param 1: " + HexString.encode(data.getParameters().get(1)));
							
							data.setFunction(NativeDriverInterface.PCSC_FUNCTION_TRANSMIT_TO_ICC);
						} else {
							virtualReaderUi.display("new PIN does not match");
						}
					}
				}
				
//				return new SimplePcscCallResult(PcscConstants.IFD_SUCCESS, new byte [] {(byte) 0x90,0});
			}
		}
		return null;
	}

	@Override
	public void setUserInterfaces(Collection<VirtualReaderUi> interfaces) {
		this.interfaces = interfaces;
	}

}
