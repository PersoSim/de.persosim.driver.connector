package de.persosim.driver.connector.features;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import de.persosim.driver.connector.NativeDriverInterface;
import de.persosim.driver.connector.UnsignedInteger;
import de.persosim.driver.connector.VirtualReaderUi;
import de.persosim.driver.connector.pcsc.AbstractPcscFeature;
import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.connector.pcsc.PcscCallResult;
import de.persosim.driver.connector.pcsc.PcscConstants;
import de.persosim.driver.connector.pcsc.SimplePcscCallResult;
import de.persosim.driver.connector.pcsc.UiEnabled;

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
				byte[] param = data.getParameters().get(0);
				
				// XXX SLS actually use data from params
				
				byte[] currentPin, newPin1, newPin2;
				boolean match;
				
				for(VirtualReaderUi virtualReaderUi : interfaces) {
					try {
						match = false;
						
						virtualReaderUi.display("Please enter current PIN");
						currentPin = virtualReaderUi.getPin();
						
						while(!match) {
							virtualReaderUi.display("Please enter new PIN");
							newPin1 = virtualReaderUi.getPin();
							
							virtualReaderUi.display("Please re-enter new PIN");
							newPin2 = virtualReaderUi.getPin();
							
							match = Arrays.equals(newPin1, newPin2) && (newPin1 != null);
							
							if(!match) {
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
