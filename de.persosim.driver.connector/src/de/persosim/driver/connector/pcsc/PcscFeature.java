package de.persosim.driver.connector.pcsc;

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
	public abstract byte[] getCapabilities();

	/**
	 * This is a concatenation of the one byte feature description, a length
	 * byte (static value of 4) and a control code to be used by the pcsc system
	 * in deviceControl executions.
	 * 
	 * @see PCSC specification part 10 2.2
	 * @return
	 */
	public abstract byte[] getFeatureDefinition();
}
