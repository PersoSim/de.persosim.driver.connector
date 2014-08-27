package de.persosim.driver.connector;

public interface PcscConstants {
	public static final int IFD_POWER_UP = 500;
	public static final int IFD_POWER_DOWN = 501;
	public static final int IFD_RESET = 502;
	public static final int IFD_NEGOTIATE_PTS1 = 1;
	public static final int IFD_NEGOTIATE_PTS2 = 2;
	public static final int IFD_NEGOTIATE_PTS3 = 4;
	public static final int IFD_SUCCESS = 0;
	public static final int IFD_ERROR_TAG = 600;
	public static final int IFD_ERROR_SET_FAILURE = 601;
	public static final int IFD_ERROR_VALUE_READ_ONLY = 602;
	public static final int IFD_ERROR_PTS_FAILURE = 605;
	public static final int IFD_ERROR_NOT_SUPPORTED = 606;
	public static final int IFD_PROTOCOL_NOT_SUPPORTED = 607;
	public static final int IFD_ERROR_POWER_ACTION = 608;
	public static final int IFD_ERROR_SWALLOW = 609;
	public static final int IFD_ERROR_EJECT = 610;
	public static final int IFD_ERROR_CONFISCATE = 611;
	public static final int IFD_COMMUNICATION_ERROR = 612;
	public static final int IFD_RESPONSE_TIMEOUT = 613;
	public static final int IFD_NOT_SUPPORTED = 614;
	public static final int IFD_ICC_PRESENT = 615;
	public static final int IFD_ICC_NOT_PRESENT = 616;
	public static final int IFD_NO_SUCH_DEVICE = 617;
	public static final int IFD_ERROR_INSUFFICIENT_BUFFER = 618;
	

	public static final int TAG_VENDOR_NAME = 0x0100;
	public static final int TAG_VENDOR_TYPE = 0x0101;
	public static final int TAG_VENDOR_VERSION = 0x0102;
	public static final int TAG_VENDOR_SERIAL = 0x0103;
	

	public static final int TAG_IFD_ATR = 0x0303;
	public static final int TAG_IFD_SLOTNUM = 0x0180;
	public static final int TAG_IFD_SLOT_THREAD_SAFE = 0x0FAC;
	public static final int TAG_IFD_THREAD_SAFE = 0x0FAD;
	public static final int TAG_IFD_SLOTS_NUMBER = 0x0FAE;
	public static final int TAG_IFD_SIMULTANEOUS_ACCESS = 0x0FAF;
	public static final int TAG_IFD_POLLING_THREAD = 0x0FB0;
	public static final int TAG_IFD_POLLING_THREAD_KILLABLE = 0x0FB1;

}
