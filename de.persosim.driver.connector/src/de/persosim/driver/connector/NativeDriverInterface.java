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
	public static final byte MESSAGE_ICC_HELLO = 0x01;
	public static final byte MESSAGE_ICC_STOP  = 0x02;
	public static final byte MESSAGE_ICC_ERROR = 0x03;
	public static final byte MESSAGE_ICC_DONE  = 0x04;
	public static final byte MESSAGE_IFD_HELLO = 0x05;
	public static final byte MESSAGE_IFD_ERROR = 0x06;
	

	public static final String MESSAGE_DIVIDER = "|";

	public static final byte PCSC_FUNCTION_DEVICE_CONTROL          = 0x10;
	public static final byte PCSC_FUNCTION_DEVICE_LIST_DEVICES     = 0x11;
	public static final byte PCSC_FUNCTION_GET_CAPABILITIES        = 0x12;
	public static final byte PCSC_FUNCTION_SET_CAPABILITIES        = 0x13;
	public static final byte PCSC_FUNCTION_POWER_ICC               = 0x14;
	public static final byte PCSC_FUNCTION_TRANSMIT_TO_ICC         = 0x15;
	public static final byte PCSC_FUNCTION_IS_ICC_PRESENT          = 0x16;
	public static final byte PCSC_FUNCTION_IS_ICC_ABSENT           = 0x17;
	public static final byte PCSC_FUNCTION_SWALLOW_ICC             = 0x18;
	public static final byte PCSC_FUNCTION_SET_PROTOCOL_PARAMETERS = 0x19;
	public static final byte PCSC_FUNCTION_LIST_INTERFACES         = 0x1A;
	public static final byte PCSC_FUNCTION_LIST_CONTEXTS           = 0x1B;
	public static final byte PCSC_FUNCTION_IS_CONTEXT_SUPPORTED    = 0x1C;
	public static final byte PCSC_FUNCTION_GET_IFDSP               = 0x1D;
	public static final byte PCSC_FUNCTION_EJECT_ICC               = 0x1E;
}
