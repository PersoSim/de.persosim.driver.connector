package de.persosim.driver.connector.pcsc;

import de.persosim.driver.connector.PcscDataHelper;
import de.persosim.simulator.utils.HexString;

public class TlvPcscCallResult implements PcscCallResult {

	private String resultData;

	public TlvPcscCallResult(int responseCode, byte[] tag, byte[] data) {
		resultData = responseCode + "#" + HexString.encode(PcscDataHelper.buildTlv(tag, data));
	}

	@Override
	public String getEncoded() {
		return resultData;
	}

}
