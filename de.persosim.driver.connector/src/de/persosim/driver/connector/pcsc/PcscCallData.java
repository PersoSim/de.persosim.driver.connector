package de.persosim.driver.connector.pcsc;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import de.persosim.driver.connector.NativeDriverInterface;
import de.persosim.driver.connector.UnsignedInteger;
import de.persosim.simulator.utils.HexString;

/**
 * This class contains all data that is transmitted when a PCSC function call
 * occurs.
 * 
 * @author mboonk
 * 
 */
public class PcscCallData {
	UnsignedInteger function;
	UnsignedInteger logicalUnitNumber;
	List<byte[]> parameters;

	/**
	 * Create a new instance by parsing the given data.
	 * @param data
	 */
	public PcscCallData(String data) {
		function = getCallType(data);
		logicalUnitNumber = getLogicalUnitNumber(data);
		parameters = getParameters(data);
		
	}

	public void setFunction(UnsignedInteger function) {
		this.function = function;
	}

	/**
	 * @return the function
	 */
	public UnsignedInteger getFunction() {
		return function;
	}

	/**
	 * @return the logicalUnitNumber
	 */
	public UnsignedInteger getLogicalUnitNumber() {
		return logicalUnitNumber;
	}

	/**
	 * @return the parameters
	 */
	public List<byte[]> getParameters() {
		return parameters;
	}

	public void setParameters(List<byte[]> parameters) {
		this.parameters = parameters;
	}

	private List<byte[]> getParameters(String data) {
		List<byte []> result = new ArrayList<byte []>();
		String [] dataArray = data.split(Pattern.quote(NativeDriverInterface.MESSAGE_DIVIDER));
		for (int i = 2; i < dataArray.length; i++){
			result.add(HexString.toByteArray(dataArray[i]));
		}
		return result;
	}

	private UnsignedInteger getLogicalUnitNumber(String data) {
		String [] dataArray = data.split(Pattern.quote(NativeDriverInterface.MESSAGE_DIVIDER));
		return UnsignedInteger.parseUnsignedInteger(dataArray[1], 16);
	}

	private UnsignedInteger getCallType(String data) {
		String [] dataArray = data.split(Pattern.quote(NativeDriverInterface.MESSAGE_DIVIDER));
		return UnsignedInteger.parseUnsignedInteger(dataArray[0], 16);
	}
}
