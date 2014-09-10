package de.persosim.driver.connector.pcsc;

import de.persosim.simulator.utils.Utils;

/**
 * This class is the parent for all {@link PcscFeature} implementations. It
 * provides the mechanism for setting a control code at instantiation time to
 * prevent clashes by arbitrary chosen control codes.
 * 
 * @author mboonk
 *
 */
public abstract class AbstractPcscFeature implements PcscFeature {
	private int controlCode;
	private byte featureTag;

	public AbstractPcscFeature(int controlCode, byte featureTag) {
		this.controlCode = controlCode;
		this.featureTag = featureTag;
	}

	/**
	 * @return the controlCode
	 */
	public int getControlCode() {
		return controlCode;
	}
	
	@Override
	public byte[] getFeatureDefinition() {
		return Utils.concatByteArrays(new byte [] {featureTag, 4}, Utils.toUnsignedByteArray(controlCode));
	}
}
