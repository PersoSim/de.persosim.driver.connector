package de.persosim.driver.connector.features;

import java.util.Arrays;

import de.persosim.driver.connector.NativeDriverInterface;
import de.persosim.driver.connector.UnsignedInteger;
import de.persosim.driver.connector.pcsc.AbstractPcscFeature;
import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.connector.pcsc.PcscCallResult;
import de.persosim.driver.connector.pcsc.PcscConstants;
import de.persosim.driver.connector.pcsc.SimplePcscCallResult;

public class ModifyPinDirect extends AbstractPcscFeature {

	public static final byte FEATURE_TAG = 0x07;
	
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
				return new SimplePcscCallResult(PcscConstants.IFD_SUCCESS, new byte [] {(byte) 0x90,0});
			}
		}
		return null;
	}

}
