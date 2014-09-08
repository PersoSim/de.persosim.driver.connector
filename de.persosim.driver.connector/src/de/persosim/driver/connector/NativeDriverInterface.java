package de.persosim.driver.connector;

import de.persosim.driver.connector.pcsc.PcscConstants;

/**
 * <p>
 * For a working connection to a native driver there are 2 protocols at work:
 * the handshake protocol used for negotiation of connection parameters and the
 * communication protocol for data exchange.
 * </p>
 * <p>
 * Handshake messages are of the form: <br/>
 * 
 * messageType|param:value|param2:value2|....|paramX:valueX <br/>
 * 
 * The possible values for the messageType can be found in the MESSAGE_*
 * constants in this class. <br/>
 * 
 * An initial Handshake consists of 4 messages: <br/>
 * 
 * HELLO_ICC|LUN:-1 if this is a new connection, if not LUN is the last used lun
 * <br/>
 * HELLO_IFD|LUN:x the IFD answers with the new lun as integer <br/>
 * DONE_ICC <br/>
 * DONE_ICC <br/>
 * 
 * To stop a channel the handshake is slightly different:
 * 
 * HELLO_ICC|LUN:x<br/>
 * HELLO_IFD|LUN:x<br/>
 * STOP_ICC<br/>
 * DONE_IFD<br/>
 * 
 * The value for x is the LUN number of the connection that is to be stopped.
 * After the native driver sent the DONE_IFD message, the socket and
 * corresponding connection should be closed.
 * </p>
 * <p>
 * Communication messages: <br/>
 * 
 * Messages contain a PCSC function encoded as a number. These numbers can be
 * found in this interface as the PCSC_FUNCTION_* constants and map directly to
 * the functions described in the PCSC specification. They are transmitted as a
 * String over a socket and besides the function number they contain the LUN and
 * parameters as necessary<br/>
 * These are encoded as hexadecimal strings and divided from each other using
 * the MESSAGE_DIVIDER.<br/>
 * The answer to such a request contains an PCSC status code as defined in
 * {@link PcscConstants} as the IFD_* constants <br/>
 * An example for this format containing 2 parameters is:</br>
 * PCSC_FUNCTION_SOME_FUNCTION|0|00112233|AABBCC
 * 
 * A result containing one parameter would be:<br/>
 * IFD_SUCCESS|12345678
 * 
 * 
 * </p>
 * 
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

	public static final String MESSAGE_DIVIDER = "|";

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
