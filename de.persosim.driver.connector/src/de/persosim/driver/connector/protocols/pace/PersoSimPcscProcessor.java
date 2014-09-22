package de.persosim.driver.connector.protocols.pace;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;

import de.persosim.driver.connector.NativeDriverInterface;
import de.persosim.driver.connector.UnsignedInteger;
import de.persosim.driver.connector.VirtualReaderUi;
import de.persosim.driver.connector.pcsc.AbstractPcscFeature;
import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.connector.pcsc.PcscCallResult;
import de.persosim.driver.connector.pcsc.PcscConstants;
import de.persosim.driver.connector.pcsc.SimplePcscCallResult;
import de.persosim.driver.connector.pcsc.SocketCommunicator;
import de.persosim.driver.connector.pcsc.UiEnabled;
import de.persosim.simulator.utils.Utils;

/**
 * This class gets PCSC style messages and processes them.
 * 
 * @author mboonk
 *
 */
public class PersoSimPcscProcessor extends AbstractPcscFeature implements SocketCommunicator, PcscConstants, UiEnabled {

	Socket communicationSocket;

	private Collection<VirtualReaderUi> interfaces;

	public static final byte FUNCTION_GET_READER_PACE_CAPABILITIES = 1;
	public static final byte FUNCTION_ESTABLISH_PACE_CHANNEL = 2;
	public static final byte FUNCTION_DESTROY_PACE_CHANNEL = 3;
	
	public static final int RESULT_NO_ERROR = 0x00000000;
	//Errors in input data 
	public static final int RESULT_INCONSITENT_LENGTHS = 0xD0000001; 
	public static final int RESULT_UNEXPECTED_DATA = 0xD0000002; 
	public static final int RESULT_UNEXPECTED_DATA_COMBINATION = 0xD0000003; 
	//Errors during protocol execution 
	public static final int RESULT_TLV_SYNTAX_ERROR = 0xE0000001; 
	public static final int RESULT_UNEXPECTED_OR_MISSING_TLV_OBJECT = 0xE0000002; 
	public static final int RESULT_UNKNOWN_PIN_ID = 0xE0000003; 
	public static final int RESULT_WRONG_AUTHENTICATION_TOKEN = 0xE0000006;
	//Response APDU of the card reports error (status code SW1SW2) 
	public static final short RESULT_PREFIX_SELECT_EF_CA = (short) 0xF000;
	public static final short RESULT_PREFIX_READ_BINARY_EF_CA = (short) 0xF001;
	public static final short RESULT_PREFIX_MSE_SET_AT = (short) 0xF002;
	public static final short RESULT_PREFIX_GAT_1 = (short) 0xF003;
	public static final short RESULT_PREFIX_GAT_2 = (short) 0xF004;
	public static final short RESULT_PREFIX_GAT_3 = (short) 0xF005;
	public static final short RESULT_PREFIX_GAT_4 = (short) 0xF006;
	//Others 
	public static final int RESULT_COMMUNICATION_ABORT = 0xF0100001;
	public static final int RESULT_NO_CARD = 0xF0100002;
	public static final int RESULT_ABORT = 0xF0200001;
	public static final int RESULT_TIMEOUT = 0xF0200002;

	public static final byte BITMAP_IFD_GENERIC_PACE_SUPPORT = 0x40;
	public static final byte BITMAP_IFD_DESTROY_CHANNEL_SUPPORT = (byte) 0x80;
	public static final byte BITMAP_EID_APPLICATION_SUPPORT = (byte) 0x20;
	public static final byte BITMAP_QUALIFIED_SIGNATURE_FUNCTION = (byte) 0x10;

	private static final int LENGTH_ESTABLISH_PACE_CHAT_LENGTH = 1;
	private static final int LENGTH_ESTABLISH_PACE_PIN_LENGTH = 1;

	private static final byte FEATURE_CONTROL_CODE = 0x20;
	
	public static final int OFFSET_FUNCTION = 0;
	public static final int OFFSET_DATA_LENGTH = 1;
	public static final int OFFSET_INPUT_DATA = 3;
	public static final int OFFSET_ESTABLISH_PACE_PIN_ID = 0;
	public static final int OFFSET_COMMAND_DATA = 1;
	public static final int OFFSET_LC = 0;

	public PersoSimPcscProcessor(UnsignedInteger controlCode) {
		super(controlCode, FEATURE_CONTROL_CODE);
	}
	
	@Override
	public PcscCallResult processPcscCall(PcscCallData data) {
		switch (data.getFunction().getAsInt()) {
		case NativeDriverInterface.VALUE_PCSC_FUNCTION_TRANSMIT_TO_ICC:
			return transmitToIcc(data);
		case NativeDriverInterface.VALUE_PCSC_FUNCTION_DEVICE_CONTROL:
			return deviceControl(data);
		default:
			return null;
		}
	}

	private PcscCallResult deviceControl(PcscCallData data) {
		UnsignedInteger controlCode = new UnsignedInteger(data.getParameters().get(0));
		UnsignedInteger expectedLength = null;
		if (2 < data.getParameters().size()){
			expectedLength = new UnsignedInteger(data.getParameters().get(2));
		}

		if (expectedLength == null){
			return new SimplePcscCallResult(PcscConstants.IFD_ERROR_INSUFFICIENT_BUFFER);
		}
		
		if (controlCode.equals(getControlCode())) {
			switch (data.getParameters().get(1)[OFFSET_FUNCTION]) {
			case FUNCTION_GET_READER_PACE_CAPABILITIES:
				return getReaderPaceCapabilities();
			case FUNCTION_ESTABLISH_PACE_CHANNEL:
				return establishPaceChannel(getInputDataFromPpdu(data.getParameters().get(1)));
			case FUNCTION_DESTROY_PACE_CHANNEL:
				return destroyPaceChannel(getInputDataFromPpdu(data.getParameters().get(1)));
			}
		}
		return null;
	}

	private PcscCallResult transmitToIcc(PcscCallData data) {
		byte[] commandPpdu = data.getParameters().get(0);
		//FIXME add check for expected length
		byte[] expectedHeader = new byte[] { (byte) 0xff, (byte) 0xc2, 0x01,
				 FEATURE_CONTROL_CODE};

		if (!Utils.arrayHasPrefix(commandPpdu, expectedHeader)) {
			return null;
		}

		switch (commandPpdu[OFFSET_FUNCTION]) {
		case FUNCTION_GET_READER_PACE_CAPABILITIES:
			return getReaderPaceCapabilities();
		case FUNCTION_ESTABLISH_PACE_CHANNEL:
			return establishPaceChannel(getInputDataFromPpdu(commandPpdu));
		case FUNCTION_DESTROY_PACE_CHANNEL:
			return destroyPaceChannel(getInputDataFromPpdu(commandPpdu));
		}
		return null;
	}

	private PcscCallResult destroyPaceChannel(byte[] inputDataFromPpdu) {
		// TODO Auto-generated method stub
		return null;
	}

	private byte [] getValue(byte [] data, int offsetLengthField, int lengthFieldLength){
		int length = Utils.getIntFromUnsignedByteArray(Arrays.copyOfRange(data, offsetLengthField, offsetLengthField + lengthFieldLength));
		return Arrays.copyOfRange(data, offsetLengthField + lengthFieldLength, offsetLengthField + lengthFieldLength + length);
	}
	
	private PcscCallResult establishPaceChannel(byte[] inputDataFromPpdu) {
		int offset = OFFSET_ESTABLISH_PACE_PIN_ID;
		//commented out while not used to prevent the unused warning
		//byte pinId = inputDataFromPpdu[OFFSET_ESTABLISH_PACE_PIN_ID];
		byte [] chat = getValue(inputDataFromPpdu, offset, LENGTH_ESTABLISH_PACE_CHAT_LENGTH);
		offset += chat.length + LENGTH_ESTABLISH_PACE_CHAT_LENGTH;
		byte [] pin = getValue(inputDataFromPpdu, offset, LENGTH_ESTABLISH_PACE_CHAT_LENGTH);
		offset += pin.length + LENGTH_ESTABLISH_PACE_PIN_LENGTH;
		
		// TODO filter pin data if the type is secret (PIN/PUK)

		if (pin.length == 0){
			for (VirtualReaderUi current : interfaces){
				try {
					pin = current.getPin();
				} catch (IOException e) {
					// TODO logging
					return buildResponse(PcscConstants.IFD_SUCCESS, RESULT_ABORT, new byte [0]);
				}
			}
		}
		
		// TODO implement PACE steps
		short mseSetAtStatusWord = 0;
		byte [] efCardAccess = new byte [0];
		byte [] currentCar = new byte [0];
		byte [] previousCar = new byte [0];
		byte [] idIcc = new byte [0];
		
		
		byte[] responseData = Utils.concatByteArrays(
				Utils.toUnsignedByteArray(mseSetAtStatusWord),
				Utils.toUnsignedByteArray((short) efCardAccess.length),
				efCardAccess, new byte[] { (byte) (0xFF & currentCar.length) },
				currentCar, new byte[] { (byte) (0xFF & previousCar.length) },
				previousCar, Utils.toUnsignedByteArray((short) idIcc.length), idIcc);
		
		return buildResponse(PcscConstants.IFD_SUCCESS, RESULT_NO_ERROR, responseData);
	}

	private PcscCallResult getReaderPaceCapabilities() {
		byte [] bitMap = new byte [] {1, BITMAP_IFD_GENERIC_PACE_SUPPORT | BITMAP_EID_APPLICATION_SUPPORT};
		return buildResponse(PcscConstants.IFD_SUCCESS, RESULT_NO_ERROR, bitMap);
	}

	private PcscCallResult buildResponse(UnsignedInteger pcscResponseCode, int result, byte[] resultData) {
		byte[] response = Utils.concatByteArrays(Utils.toUnsignedByteArray(result),
				Utils.toUnsignedByteArray((short) resultData.length),
				resultData);
		return new SimplePcscCallResult(pcscResponseCode, response);
	}

	private byte[] getInputDataFromPpdu(byte[] ppdu) {
		return Arrays.copyOfRange(ppdu, OFFSET_INPUT_DATA, Utils
				.getShortFromUnsignedByteArray(Arrays.copyOfRange(ppdu,
						OFFSET_DATA_LENGTH, OFFSET_INPUT_DATA + OFFSET_COMMAND_DATA)));
	}

	@Override
	public void setCommunicationSocket(Socket socket) {
		this.communicationSocket = socket;
	}

	@Override
	public byte[] getCapabilities() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setUserInterfaces(Collection<VirtualReaderUi> interfaces) {
		this.interfaces = interfaces;
	}
}
