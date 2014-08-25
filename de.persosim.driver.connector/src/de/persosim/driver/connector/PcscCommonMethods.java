package de.persosim.driver.connector;

import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.connector.pcsc.PcscCallResult;

public interface PcscCommonMethods {
	
	/**
	 * @see PCSC Specification Part 3 4.2
	 * @return a list of all device descriptors exposed
	 */
	public abstract PcscCallResult deviceListDevices();
	
	public abstract PcscCallResult deviceControl(PcscCallData data);
}
