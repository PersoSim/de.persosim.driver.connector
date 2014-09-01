package de.persosim.driver.connector.pcsc;


public interface PcscSlotLogicalDeviceMethods {
	//Administrative
	public abstract PcscCallResult getCapabilities(PcscCallData data);
	public abstract PcscCallResult setCapabilities(PcscCallData data);
	public abstract PcscCallResult setProtocolParameters(PcscCallData data);
	public abstract PcscCallResult powerIcc(PcscCallData data);
	public abstract PcscCallResult swallowIcc(PcscCallData data);
	public abstract PcscCallResult ejectIcc(PcscCallData data);
	public abstract PcscCallResult isIccPresent(PcscCallData data);
	public abstract PcscCallResult isIccAbsent(PcscCallData data);
	
	//IFDSP related
	public abstract PcscCallResult listContexts(PcscCallData data);
	public abstract PcscCallResult isContextSupported(PcscCallData data);
	public abstract PcscCallResult getIfdsp(PcscCallData data);
	public abstract PcscCallResult listInterfaces(PcscCallData data);
	
}
