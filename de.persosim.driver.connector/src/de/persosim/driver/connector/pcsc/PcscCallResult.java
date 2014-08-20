package de.persosim.driver.connector.pcsc;

/**
 * This interface describes the result data for a PCSC function call.
 * 
 * @author mboonk
 * 
 */
public interface PcscCallResult {
	/**
	 * @return the data encoded for sending to the native driver
	 */
	public String getEncoded();
}
