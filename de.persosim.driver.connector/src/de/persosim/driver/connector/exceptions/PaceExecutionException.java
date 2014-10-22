package de.persosim.driver.connector.exceptions;

import de.persosim.driver.connector.UnsignedInteger;

public class PaceExecutionException extends Exception {

	private static final long serialVersionUID = 1L;

	private UnsignedInteger errorCode;
	private byte [] responseBuffer;
	
	public PaceExecutionException(UnsignedInteger errorCode, byte [] responseBuffer){
		this.errorCode = errorCode;
		this.responseBuffer = responseBuffer;
	}
	
	public UnsignedInteger getError(){
		return errorCode;
	}
	
	public byte [] getResponseBuffer(){
		return responseBuffer;
	}
}
