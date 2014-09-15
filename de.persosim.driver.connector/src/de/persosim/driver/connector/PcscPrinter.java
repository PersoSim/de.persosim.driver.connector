package de.persosim.driver.connector;

import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.connector.pcsc.PcscCallResult;
import de.persosim.driver.connector.pcsc.PcscListener;
import de.persosim.simulator.utils.HexString;

/**
 * This @link {@link PcscListener} prints all received PCSC data to standard
 * out.
 * 
 * @author mboonk
 * 
 */
public class PcscPrinter implements PcscListener {

	@Override
	public PcscCallResult processPcscCall(PcscCallData data) {
		System.out.print("PCSC Printer:\t" + HexString.encode(data.getFunction()) + NativeDriverInterface.MESSAGE_DIVIDER + HexString.encode(data.getLogicalUnitNumber())
				+ NativeDriverInterface.MESSAGE_DIVIDER);
		for (byte[] current : data.getParameters()) {
			System.out.println(NativeDriverInterface.MESSAGE_DIVIDER + HexString.encode(current));
		}
		return null;
	}

}
