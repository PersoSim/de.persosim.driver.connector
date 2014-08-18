package de.persosim.driver.connector.pcsc;

import de.persosim.simulator.tlv.TlvDataObject;

/**
 * This interface defines a PCSC feature.
 * 
 * @author mboonk
 * 
 */
public interface PcscFeature {
	/**
	 * Implementations should use this method to process the data received from
	 * the native PCSC system.
	 * 
	 * @param controlCode
	 *            the functions control code
	 * @param data
	 *            the input data
	 * @return the result of this features calculations as byte array
	 */
	public abstract byte[] process(byte[] controlCode, byte[] data);

	/**
	 * @return the capabilities provided by this PCSC feature
	 */
	public abstract TlvDataObject getCapabilities();

	/**
	 * @see PCSC specification part 10 2.2
	 * @return
	 */
	public abstract TlvDataObject getFeatureDefinition();
}
