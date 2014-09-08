package de.persosim.driver.connector.pcsc;

import java.util.Arrays;

import de.persosim.simulator.utils.Utils;

/**
 * This class contains several helper methods for pcsc data processing.
 * 
 * @author mboonk
 *
 */
public class PcscDataHelper {
	public static final int DWORD_LENGTH = 4;
	public static final int BOOL_LENGTH = 2;

	/**
	 * @param value
	 * @return the given value encoded as PCSC boolean value
	 */
	public static byte[] getBool(boolean value) {
		// FIXME these values are not confirmed, check them
		if (value) {
			return new byte[] { 0, 1 };
		}
		return new byte[] { 0, 0 };
	}

	/**
	 * @param bool
	 *            the value to be parsed, the array must have at least a length
	 *            of offset + 2
	 * @param offset
	 * @return true, iff bool[offset] and bool[offset + 1] are not both equal to
	 *         0
	 */
	public static boolean parseBool(byte[] bool, int offset) {
		if (bool[offset] == 0 && bool[offset + 1] == 0) {
			return false;
		}
		return true;
	}

	private static byte[] getPaddedTag(byte[] tag) {
		byte[] tagPadding = new byte[0];
		if (tag.length < DWORD_LENGTH) {
			tagPadding = new byte[DWORD_LENGTH - tag.length];
		}
		return Utils.concatByteArrays(tagPadding, tag);
	}

	/**
	 * Builds a tlv encoding using 4 bytes for the tag, 4 bytes for the length
	 * and an arbitrary length value field.
	 * 
	 * @param tag
	 * @param value
	 * @return the tlv encoding
	 */
	public static byte[] buildTlv(byte[] tag, byte[] value) {
		// FIXME check in which cases setting the valid boolean to false makes
		// sense
		return Utils.concatByteArrays(getPaddedTag(tag),
				Utils.toUnsignedByteArray(value.length), value, getBool(true));
	}

	private static boolean compare(byte[] a1, int a1Offset, byte[] a2,
			int a2Offset, int length) {
		for (int i = 0; i < length; i++) {
			if (a1[i + a1Offset] != a2[i + a2Offset]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @param tag
	 * @param data
	 * @return the content as byte [] or null if the tag could not be found
	 */
	public static byte[] getFieldContent(byte[] tag, byte[] data) {
		byte[] paddedTag = getPaddedTag(tag);
		int offset = 0;
		int length = 0;
		while (offset < data.length - DWORD_LENGTH) {
			length = Utils.getIntFromUnsignedByteArray(Arrays.copyOfRange(data,
					offset + DWORD_LENGTH, offset + DWORD_LENGTH * 2));
			if (compare(paddedTag, 0, data, offset, DWORD_LENGTH)) {
				return Arrays.copyOfRange(data, offset, offset + length);
			} else {
				offset += length;
			}
		}
		return null;
	}

	/**
	 * @param tag
	 * @param data
	 * @return the complete tag field (tag|length|content) as byte [] or null if
	 *         the tag could not be found
	 */
	public static byte[] getField(byte[] tag, byte[] data) {
		byte[] paddedTag = getPaddedTag(tag);
		int offset = 0;
		int length = 0;
		while (offset < data.length - DWORD_LENGTH) {
			length = Utils.getIntFromUnsignedByteArray(Arrays.copyOfRange(data,
					offset + DWORD_LENGTH, offset + DWORD_LENGTH * 2));
			if (compare(paddedTag, 0, data, offset, DWORD_LENGTH)) {
				return Arrays.copyOfRange(data, offset, offset + length);
			} else {
				offset += length;
			}
		}
		return null;
	}
}
