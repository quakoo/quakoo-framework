/**
 * 
 */
package com.quakoo.baseFramework.util;

/**
 * 
 * @author LiYongbiao1
 *
 */
public class ByteUtil {

	/**
	 * Size of boolean in bytes
	 */
	public static final int SIZEOF_BOOLEAN = Byte.SIZE / Byte.SIZE;

	/**
	 * Size of byte in bytes
	 */
	public static final int SIZEOF_BYTE = SIZEOF_BOOLEAN;

	/**
	 * Size of char in bytes
	 */
	public static final int SIZEOF_CHAR = Character.SIZE / Byte.SIZE;

	/**
	 * Size of double in bytes
	 */
	public static final int SIZEOF_DOUBLE = Double.SIZE / Byte.SIZE;

	/**
	 * Size of float in bytes
	 */
	public static final int SIZEOF_FLOAT = Float.SIZE / Byte.SIZE;

	/**
	 * Size of int in bytes
	 */
	public static final int SIZEOF_INT = Integer.SIZE / Byte.SIZE;

	/**
	 * Size of long in bytes
	 */
	public static final int SIZEOF_LONG = Long.SIZE / Byte.SIZE;

	/**
	 * Size of short in bytes
	 */
	public static final int SIZEOF_SHORT = Short.SIZE / Byte.SIZE;

	/**
	 * Estimate of size cost to pay beyond payload in jvm for instance of byte
	 * []. Estimate based on study of jhat and jprofiler numbers.
	 */
	// JHat says BU is 56 bytes.
	// SizeOf which uses java.lang.instrument says 24 bytes. (3 longs?)
	public static final int ESTIMATED_HEAP_TAX = 16;

	public static void putInt(byte[] bb, int x, int index) {
		bb[index + 0] = (byte) (x >> 24);
		bb[index + 1] = (byte) (x >> 16);
		bb[index + 2] = (byte) (x >> 8);
		bb[index + 3] = (byte) (x >> 0);
	}

	public static byte[] putInt(int x) {
		byte[] bb = new byte[SIZEOF_INT];
		putInt(bb, x, 0);
		return bb;
	}

	public static int getInt(byte[] bb, int index) {
		return (int) ((((bb[index + 0] & 0xff) << 24)
				| ((bb[index + 1] & 0xff) << 16)
				| ((bb[index + 2] & 0xff) << 8) | ((bb[index + 3] & 0xff) << 0)));
	}

	public static void putLong(byte[] bb, long x, int index) {
		bb[index + 0] = (byte) (x >> 56);
		bb[index + 1] = (byte) (x >> 48);
		bb[index + 2] = (byte) (x >> 40);
		bb[index + 3] = (byte) (x >> 32);
		bb[index + 4] = (byte) (x >> 24);
		bb[index + 5] = (byte) (x >> 16);
		bb[index + 6] = (byte) (x >> 8);
		bb[index + 7] = (byte) (x >> 0);
	}

	public static byte[] putLong(long x) {
		byte[] bb = new byte[SIZEOF_LONG];
		putLong(bb, x, 0);
		return bb;
	}

	public static long getLong(byte[] bb, int index) {
		return ((((long) bb[index + 0] & 0xff) << 56)
				| (((long) bb[index + 1] & 0xff) << 48)
				| (((long) bb[index + 2] & 0xff) << 40)
				| (((long) bb[index + 3] & 0xff) << 32)
				| (((long) bb[index + 4] & 0xff) << 24)
				| (((long) bb[index + 5] & 0xff) << 16)
				| (((long) bb[index + 6] & 0xff) << 8) | (((long) bb[index + 7] & 0xff) << 0));
	}



	/**
	 * test if the bit at the specific position is 1
	 * 
	 * @param b
	 *            byte data
	 * @param pos
	 *            position started with 0, and less than 8
	 * @return bit value,0 or 1
	 */
	public static int test(byte b, int pos) {
		byte mask = 1;
		return ((mask << pos) & b) >> pos;
	}

	/**
	 * set the specific bit to 1
	 * 
	 * @param b
	 *            byte data
	 * @param pos
	 *            position started with 0, and less than 8
	 * @return byte data
	 */
	public static byte set(byte b, int pos) {
		byte mask = 1;
		b |= mask << pos;
		return b;
	}

	/**
	 * clear the specific bit to 0
	 * 
	 * @param b
	 *            byte value
	 * @param pos
	 *            position started with 0, and less than 8
	 * @return byte data
	 */
	public static byte clear(byte b, int pos) {
		byte mask = 1;
		b &= ~(mask << pos);
		return b;
	}


	public static float getFloat(byte[] bytes)
	{
		return Float.intBitsToFloat(getInt(bytes,0));
	}

	public static double getDouble(byte[] bytes)
	{
		long l = getLong(bytes,0);
		return Double.longBitsToDouble(l);
	}
	public static short getShort(byte[] bytes)
	{
		return (short) ((0xff & bytes[0]) | (0xff00 & (bytes[1] << 8)));
	}

	public static char getChar(byte[] bytes)
	{
		return (char) ((0xff & bytes[0]) | (0xff00 & (bytes[1] << 8)));
	}

	public static byte[] putShort(short data)
	{
		byte[] bytes = new byte[2];
		bytes[0] = (byte) (data & 0xff);
		bytes[1] = (byte) ((data & 0xff00) >> 8);
		return bytes;
	}

	public static byte[] putChar(char data)
	{
		byte[] bytes = new byte[2];
		bytes[0] = (byte) (data);
		bytes[1] = (byte) (data >> 8);
		return bytes;
	}

	public static byte[] putFloat(float data)
	{
		int intBits = Float.floatToIntBits(data);
		return putInt(intBits);
	}

	public static byte[] putDouble(double data)
	{
		long intBits = Double.doubleToLongBits(data);
		return putLong(intBits);
	}




}
