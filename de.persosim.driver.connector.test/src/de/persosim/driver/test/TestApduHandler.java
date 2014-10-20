package de.persosim.driver.test;


/**
 * This interface is used to describe the APDU handling capabilities of the
 * {@link TestSocketSim}.
 * 
 * @author mboonk
 *
 */
public interface TestApduHandler {

	/**
	 * This method processes a given APDU as an hexadecimal formatted string
	 * and returns a response.
	 * 
	 * @param apduLine the hex string encoded APDU
	 * @return the response-APDU, also encoded as a hex string 
	 */
	String processCommand(String apduLine);

}
