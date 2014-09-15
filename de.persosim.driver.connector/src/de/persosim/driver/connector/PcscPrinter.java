package de.persosim.driver.connector;

import java.lang.reflect.Field;

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
		System.out.print("PCSC Printer:\t" + getStringRep(data.getFunction()) + NativeDriverInterface.MESSAGE_DIVIDER + HexString.encode(data.getLogicalUnitNumber()));
		for (byte[] current : data.getParameters()) {
			System.out.print(NativeDriverInterface.MESSAGE_DIVIDER + HexString.encode(current));
		}
		System.out.println();
		
		return null;
	}
	
	String getStringRep(byte value){
		Field [] fields = NativeDriverInterface.class.getDeclaredFields();
		for (Field field : fields){
			try {
				if (value == field.getByte(new NativeDriverInterface() {
				})){
					return field.getName();
				}
			} catch (Exception e) {
				continue;
			}
		}
		return HexString.encode(value);
	}

}
