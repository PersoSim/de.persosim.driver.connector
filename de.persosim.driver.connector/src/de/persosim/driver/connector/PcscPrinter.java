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
		System.out.print("PCSC Printer:\t" + data.getFunction() + "|" + data.getLogicalUnitNumber()
				+ "|");
		for (byte[] current : data.getParameters()) {
			System.out.println("Param:" + HexString.encode(current));
		}
		return null;
	}

}
