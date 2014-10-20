package de.persosim.driver.connector.features;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import de.persosim.driver.connector.CommUtils;
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
import de.persosim.simulator.platform.Iso7816Lib;
import de.persosim.simulator.tlv.PrimitiveTlvDataObject;
import de.persosim.simulator.tlv.TlvConstants;
import de.persosim.simulator.tlv.TlvDataObject;
import de.persosim.simulator.tlv.TlvDataObjectContainer;
import de.persosim.simulator.tlv.TlvTag;
import de.persosim.simulator.utils.HexString;
import de.persosim.simulator.utils.Utils;

/**
 * This class gets PCSC style messages and processes them.
 * 
 * @author mboonk
 *
 */
public class PersoSimPcscProcessor extends AbstractPcscFeature implements SocketCommunicator, PcscConstants, UiEnabled {

	public enum PaceState {
		NO_SM, SM_ESTABLISHED;
	}
	
	Socket communicationSocket;

	private List<VirtualReaderUi> interfaces;

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
	public static final int OFFSET_ESTABLISH_PACE_PIN_ID = 3;
	public static final int OFFSET_COMMAND_DATA = 1;
	public static final int OFFSET_LC = 0;

	private PaceState currentState = PaceState.NO_SM;

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
		case NativeDriverInterface.VALUE_PCSC_FUNCTION_POWER_ICC:
			return powerIcc(data);
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
				return establishPaceChannel(data.getParameters().get(1));
			case FUNCTION_DESTROY_PACE_CHANNEL:
				return destroyPaceChannel(data.getParameters().get(1));
			}
		}
		return null;
	}



	private PcscCallResult powerIcc(PcscCallData data) {
		UnsignedInteger action = new UnsignedInteger(data.getParameters().get(0));
		
		if (IFD_POWER_DOWN.equals(action) || IFD_POWER_UP.equals(action)
				|| IFD_RESET.equals(action)) {
			currentState = PaceState.NO_SM;
		}
		return null;
	}
	
	/**
	 * An APDU will be modified by setting the logical channel bits to 1 but not
	 * send. This method can also be used to execute PACE commands using PPDUs
	 * with an FFC20120 prefix.
	 * 
	 * @param data
	 * @return
	 */
	private PcscCallResult transmitToIcc(PcscCallData data) {
		byte[] commandPpdu = data.getParameters().get(0);

		UnsignedInteger expectedLength = CommUtils.getExpectedLength(data, 1);

		if (expectedLength == null){
			return new SimplePcscCallResult(PcscConstants.IFD_ERROR_INSUFFICIENT_BUFFER);
		}
		
		byte[] expectedHeader = new byte[] { (byte) 0xff, (byte) 0xc2, 0x01,
				 FEATURE_CONTROL_CODE};

		if (!Utils.arrayHasPrefix(commandPpdu, expectedHeader)) {
			if (currentState.equals(PaceState.SM_ESTABLISHED)){
				// destroy the channel when a secure messaging apdu arrives
				if ((commandPpdu[Iso7816Lib.OFFSET_CLA] & 0x0c) == 0x0c){
					currentState = PaceState.NO_SM;
					return null;
				}
				//set logical channel to 3 
				commandPpdu[Iso7816Lib.OFFSET_CLA] |= 0b011;
			}
			
			return null;
		}

		PcscCallResult result = null;
		switch (commandPpdu[OFFSET_FUNCTION]) {
		case FUNCTION_GET_READER_PACE_CAPABILITIES:
			result = getReaderPaceCapabilities();
		case FUNCTION_ESTABLISH_PACE_CHANNEL:
			result = establishPaceChannel(getInputDataFromPpdu(commandPpdu));
		case FUNCTION_DESTROY_PACE_CHANNEL:
			result =  destroyPaceChannel(getInputDataFromPpdu(commandPpdu));
		}

		if (result != null && result.getEncoded().length() - 9 > expectedLength.getAsSignedLong()){
			return new SimplePcscCallResult(PcscConstants.IFD_ERROR_INSUFFICIENT_BUFFER);
		}
		
		return null;
	}

	private PcscCallResult destroyPaceChannel(byte[] inputDataFromPpdu) {
		// not supported
		return null;
	}

	private byte [] getValue(byte [] data, int offsetLengthField, int lengthFieldLength){
		int length = Utils.getIntFromUnsignedByteArray(Arrays.copyOfRange(data, offsetLengthField, offsetLengthField + lengthFieldLength));
		return Arrays.copyOfRange(data, offsetLengthField + lengthFieldLength, offsetLengthField + lengthFieldLength + length);
	}
	
	private PcscCallResult establishPaceChannel(byte[] inputDataFromPpdu) {
		int offset = OFFSET_ESTABLISH_PACE_PIN_ID;
		byte pinId = inputDataFromPpdu[offset];
		byte[] chat = getValue(inputDataFromPpdu, ++offset,
				LENGTH_ESTABLISH_PACE_CHAT_LENGTH);
		offset += chat.length + LENGTH_ESTABLISH_PACE_CHAT_LENGTH;
		byte[] pin = getValue(inputDataFromPpdu, offset,
				LENGTH_ESTABLISH_PACE_PIN_LENGTH);
		offset += pin.length + LENGTH_ESTABLISH_PACE_PIN_LENGTH;

		try {
			if (pin.length == 0) {
				pin = getPinFromInterfaces(pin);

			}
		} catch (IOException e) {
			// TODO logging
			return new SimplePcscCallResult(IFD_ERROR_NOT_SUPPORTED);
		}

		if (pin == null) {
			return buildResponse(PcscConstants.IFD_SUCCESS, RESULT_ABORT,
					new byte[0]);
		}

		TlvDataObjectContainer data = buildPseudoApduData(pinId, chat, pin);

		try {

			byte[] efCardAccess = readCardAccess();

			byte[] pseudoApduHeader = new byte[] { (byte) 0xff, (byte) 0x86, 0,
					0, (byte) (0xFF & data.toByteArray().length) };
			byte[] responseApdu = CommUtils.exchangeApdu(communicationSocket, Utils
					.concatByteArrays(pseudoApduHeader, data.toByteArray()));

			byte[] pcscResponse = buildPcscResponseData(efCardAccess, responseApdu);
			return buildResponse(PcscConstants.IFD_SUCCESS, RESULT_NO_ERROR,
					pcscResponse);

		} catch (IOException e) {
			// TODO logging
			return buildResponse(PcscConstants.IFD_SUCCESS,
					RESULT_COMMUNICATION_ABORT, new byte[0]);
		}
		
	}

	private byte[] getPinFromInterfaces(byte[] pin) throws IOException {
		if (interfaces != null) {
			for (VirtualReaderUi current : interfaces) {
				current.display("Enter PACE password");
				pin = current.getPin();
				if (pin != null) {
					break;
				}
			}
			return pin;
		}
		return null;
	}

	private byte[] buildPcscResponseData(byte[] efCardAccess, byte[] responseApdu) {
		TlvDataObjectContainer responseData = new TlvDataObjectContainer(
				Arrays.copyOf(responseApdu, responseApdu.length - 2));

		byte[] mseSetAtStatusWord = Arrays.copyOfRange(responseApdu,
				responseApdu.length - 2, responseApdu.length);
		TlvDataObject currentCar = responseData
				.getTlvDataObject(TlvConstants.TAG_87);
		TlvDataObject previousCar = responseData
				.getTlvDataObject(TlvConstants.TAG_88);
		TlvDataObject idIcc = responseData
				.getTlvDataObject(TlvConstants.TAG_86);

		if (currentCar == null) {
			currentCar = new PrimitiveTlvDataObject(new TlvTag((byte) 0));
		}
		if (previousCar == null) {
			previousCar = new PrimitiveTlvDataObject(new TlvTag((byte) 0));
		}
		if (idIcc == null) {
			idIcc = new PrimitiveTlvDataObject(new TlvTag((byte) 0));
		}

		byte[] pcscResponse = Utils
				.concatByteArrays(
						mseSetAtStatusWord,
						CommUtils
								.toUnsignedShortFlippedBytes((short) efCardAccess.length),
						efCardAccess,
						new byte[] { (byte) (0xFF & currentCar
								.getLengthValue()) }, currentCar
								.getValueField(),
						new byte[] { (byte) (0xFF & previousCar
								.getLengthValue()) }, previousCar
								.getValueField(), CommUtils
								.toUnsignedShortFlippedBytes((short) idIcc
										.getLengthValue()), idIcc
								.getValueField());

		currentState = PaceState.SM_ESTABLISHED;
		return pcscResponse;
	}

	private byte[] readCardAccess() throws IOException {
		byte [] select = HexString.toByteArray("00 A4 02 0C 02 01 1C");
		byte [] readBinary = HexString.toByteArray("00 B0 00 00 00");
		
		byte [] response = CommUtils.exchangeApdu(communicationSocket, select);
		
		if (Iso7816Lib.isReportingError(Utils.getShortFromUnsignedByteArray(Arrays.copyOfRange(response, response.length - 2, response.length - 1)))){
			// TODO logging
			throw new IOException("Selection efCardAccess unsuccessfull");
		}
		response = CommUtils.exchangeApdu(communicationSocket, readBinary);
		
			
		if (Iso7816Lib.isReportingError(Utils.getShortFromUnsignedByteArray(Arrays.copyOfRange(response, response.length - 2, response.length - 1)))){
			// TODO logging
			throw new IOException("Read binary of efCardAccess unsuccessfull");
		}
		
		return Arrays.copyOf(response, response.length - 2);
	}

	private TlvDataObjectContainer buildPseudoApduData(byte pinId, byte[] chat,
			byte[] pin) {
		TlvDataObjectContainer data = new TlvDataObjectContainer();
		//add password id
		data.addTlvDataObject(new PrimitiveTlvDataObject(TlvConstants.TAG_83, new byte [] { pinId }));
		//add password data
		data.addTlvDataObject(new PrimitiveTlvDataObject(TlvConstants.TAG_92, pin));
		//add chat
		if (chat.length > 0){
			data.addTlvDataObject(new PrimitiveTlvDataObject(TlvConstants.TAG_7F4C, chat));	
		}
		return data;
	}

	private PcscCallResult getReaderPaceCapabilities() {
		byte [] bitMap = new byte [] {BITMAP_IFD_GENERIC_PACE_SUPPORT | BITMAP_EID_APPLICATION_SUPPORT};
		return buildResponse(PcscConstants.IFD_SUCCESS, RESULT_NO_ERROR, bitMap);
	}

	private PcscCallResult buildResponse(UnsignedInteger pcscResponseCode, int result, byte[] resultData) {
		byte[] response = Utils.concatByteArrays(Utils.toUnsignedByteArray(result),
				CommUtils.toUnsignedShortFlippedBytes((short) resultData.length),
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
	public void setUserInterfaces(List<VirtualReaderUi> interfaces) {
		this.interfaces = interfaces;
	}
}
