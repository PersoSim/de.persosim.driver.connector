package de.persosim.driver.connector.pcsc;

import de.persosim.simulator.utils.HexString;

public class SimplePcscCallResult implements PcscCallResult {
	private String resultData;
	
	public SimplePcscCallResult(int responseCode){
		resultData = responseCode + "";
	}
	
	public SimplePcscCallResult(int responseCode, byte [] ... data) {
		this(responseCode);
		for (byte[] current : data) {
			resultData += "#" + HexString.encode(current);
		}
	}
	
	@Override
	public String getEncoded() {
		return resultData;
	}

}
