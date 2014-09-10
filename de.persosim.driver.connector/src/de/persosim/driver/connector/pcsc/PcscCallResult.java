package de.persosim.driver.connector.pcsc;

/**
 * This interface describes the result data for a PCSC function call.
 * 
 * @author mboonk
 * 
 */
public interface PcscCallResult {
	/**
	 * The format used for this response is:
	 * RESPONSECODE|[data]
	 * <br/>
	 * [data] consists of a concatenation of byte arrays divided by |
	 * 
	 * @return the data encoded for sending to the native driver
	 */
	public String getEncoded();
}
