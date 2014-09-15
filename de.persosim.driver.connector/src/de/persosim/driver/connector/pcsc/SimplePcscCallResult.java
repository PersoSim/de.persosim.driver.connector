package de.persosim.driver.connector.pcsc;

import de.persosim.driver.connector.NativeDriverInterface;
import de.persosim.simulator.utils.HexString;
import de.persosim.simulator.utils.Utils;

/**
 * This {@link PcscCallResult} contains a status code and optional data parameters.
 * @author mboonk
 *
 */
public class SimplePcscCallResult implements PcscCallResult {
	private String resultData;
	
	public SimplePcscCallResult(int responseCode){
		resultData = HexString.encode(Utils.toUnsignedByteArray(responseCode));
	}
	
	public SimplePcscCallResult(int responseCode, byte [] ... data) {
		this(responseCode);
		for (byte[] current : data) {
			resultData += NativeDriverInterface.MESSAGE_DIVIDER + HexString.encode(current);
		}
	}
	
	@Override
	public String getEncoded() {
		return resultData;
	}

}
