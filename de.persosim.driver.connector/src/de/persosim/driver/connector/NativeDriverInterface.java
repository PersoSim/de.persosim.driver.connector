package de.persosim.driver.connector;

/**
 * @author mboonk
 *
 */
public interface NativeDriverInterface {
	public static final String MESSAGE_ICC_HELLO = "HELLO_ICC"; // Example:
	// HELLO_ICC|LUN:-1
	public static final String MESSAGE_ICC_STOP = "STOP_ICC";
	public static final String MESSAGE_ICC_ERROR = "ERROR_ICC";
	public static final String MESSAGE_ICC_DONE = "DONE_ICC";
	public static final String MESSAGE_IFD_HELLO = "HELLO_IFD"; // Example:
	// HELLO_IFD|LUN:3
	public static final String MESSAGE_IFD_DONE = "DONE_IFD";
	public static final String MESSAGE_IFD_ERROR = "ERROR_IFD";

	public static final String TYPE_STR = "STR";
	public static final String TYPE_DWORD = "DWORD";
	public static final String TYPE_TLV = "TLV";
	public static final String TYPE_BYTE_ARRAY = "BYTE_ARRAY";

	public static final int PCSC_FUNCTION_DEVICE_CONTROL = 0;
	public static final int PCSC_FUNCTION_DEVICE_LIST_DEVICES = 1;
	public static final int PCSC_FUNCTION_GET_CAPABILITIES = 2;
	public static final int PCSC_FUNCTION_SET_CAPABILITIES = 3;
	public static final int PCSC_FUNCTION_POWER_ICC = 4;
	public static final int PCSC_FUNCTION_TRANSMIT_TO_ICC = 5;
	public static final int PCSC_FUNCTION_IS_ICC_PRESENT = 6;
}
