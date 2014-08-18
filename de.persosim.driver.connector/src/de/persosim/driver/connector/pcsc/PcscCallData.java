package de.persosim.driver.connector.pcsc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class contains all data that is transmitted when a PCSC function call
 * occurs.
 * 
 * @author mboonk
 * 
 */
public class PcscCallData {
	int function;
	int logicalUnitNumber;
	List<byte[]> parameters;

	public PcscCallData(String data) {
		function = getCallType(data);
		logicalUnitNumber = getLogicalUnitNumber(data);
		parameters = getParameters(data);
	}

	/**
	 * @return the function
	 */
	public int getFunction() {
		return function;
	}

	/**
	 * @return the logicalUnitNumber
	 */
	public int getLogicalUnitNumber() {
		return logicalUnitNumber;
	}

	/**
	 * @return the parameters
	 */
	public List<byte[]> getParameters() {
		List<byte[]> result = new ArrayList<byte[]>();
		for (byte[] array : parameters) {
			result.add(Arrays.copyOf(array, array.length));
		}
		return result;
	}

	private List<byte[]> getParameters(String data) {
		// FIXME MBK implement
		return null;
	}

	private int getLogicalUnitNumber(String data) {
		// FIXME MBK implement
		return 0;
	}

	private int getCallType(String data) {
		// FIXME MBK implement
		return 0;
	}
}
