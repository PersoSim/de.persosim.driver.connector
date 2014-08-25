package de.persosim.driver.connector.pcsc;

import de.persosim.simulator.tlv.TlvDataObject;

/**
 * This interface defines a PCSC feature.
 * 
 * @author mboonk
 * 
 */
public interface PcscFeature extends PcscListener {
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
