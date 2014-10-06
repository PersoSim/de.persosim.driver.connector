package de.persosim.driver.connector;

import de.persosim.simulator.utils.HexString;
import de.persosim.simulator.utils.Utils;

public class UnsignedInteger {
	private long value;
	private static final long MAX_VALUE_LONG = 0x0FFFFFFFFl;
	private static final long MIN_VALUE_LONG = 0;
	public static final UnsignedInteger MAX_VALUE = new UnsignedInteger(MAX_VALUE_LONG);
	public static final UnsignedInteger MIN_VALUE = new UnsignedInteger(MIN_VALUE_LONG);
	
	public UnsignedInteger(long value){
		if (value > MAX_VALUE_LONG || value < 0 ){
			throw new NumberFormatException("Value to big for this type");
		}
		this.value = value;
	}
	
	public UnsignedInteger(int value){
		this.value = value & 0xFFFFFFFFl;
	}
	
	public UnsignedInteger(byte [] data){
		if (data.length > 4){
			throw new NumberFormatException("Source byte array to long");
		}
		this.value = Utils.getIntFromUnsignedByteArray(data) & 0xFFFFFFFFl;
	}
	
	public int getAsInt(){
		return (int) value;
	}
	
	public static UnsignedInteger parseUnsignedInteger(String value, int radix){
		long newValue = Long.parseLong(value, radix);
		return new UnsignedInteger(newValue);
	}
	
	public String getAsHexString(){
		return HexString.encode(getAsByteArray());
	}
	
	public byte [] getAsByteArray(){
		return Utils.toUnsignedByteArray(getAsInt());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (value ^ (value >>> 32));
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UnsignedInteger other = (UnsignedInteger) obj;
		if (value != other.value)
			return false;
		return true;
	}

	public long getAsSignedLong() {
		return value;
	}
}
