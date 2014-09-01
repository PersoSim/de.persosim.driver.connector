package de.persosim.driver.connector.pcsc;


public interface PcscCommunicationServices {
	public PcscCallResult transmitToIcc(PcscCallData data);
}
