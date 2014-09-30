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
				byte[] encodedLengthBytes = Arrays.copyOfRange(param, 20, 24);
				byte[] apduHeaderBytes = Arrays.copyOfRange(param, 24, param.length);
				
				System.out.println("Param 1: " + HexString.encode(param));
				System.out.println("expected length: " + HexString.encode(encodedLengthBytes));
				System.out.println("APDU header: " + HexString.encode(apduHeaderBytes));
				
				// XXX SLS actually use data from params
				
				byte[] resetRetryCounterApduBytes;
				
				byte[] newPin1, newPin2;
				boolean match;
				
				for(VirtualReaderUi virtualReaderUi : interfaces) {
					try {
						match = false;
						
						while(!match) {
							virtualReaderUi.display("Please enter new PIN");
							newPin1 = virtualReaderUi.getPin();
							
							virtualReaderUi.display("Please re-enter new PIN");
							newPin2 = virtualReaderUi.getPin();
							
							match = Arrays.equals(newPin1, newPin2) && (newPin1 != null);
							
							if(match) {
								resetRetryCounterApduBytes = Utils.concatByteArrays(param, HexString.toByteArray(HexString.hexifyByte(newPin1.length)), newPin1);
								params.set(1, resetRetryCounterApduBytes);
								System.out.println("end: " + HexString.encode(params.get(1)));
								System.out.println("modified param 1: " + HexString.encode(data.getParameters().get(1)));
							} else {
								virtualReaderUi.display("new PIN does not match");
							}
						}
						
						
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				return new SimplePcscCallResult(PcscConstants.IFD_SUCCESS, new byte [] {(byte) 0x90,0});
			}
		}
		return null;
	}

	@Override
	public void setUserInterfaces(Collection<VirtualReaderUi> interfaces) {
		this.interfaces = interfaces;
	}

}
