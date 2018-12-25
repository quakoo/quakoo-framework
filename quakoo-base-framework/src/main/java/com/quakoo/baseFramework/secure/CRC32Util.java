package com.quakoo.baseFramework.secure;

import java.util.zip.CRC32;

public class CRC32Util{

	public static int crc32(byte[] bytes){
		CRC32 crc32=new CRC32();
		crc32.update(bytes);
		return (int)crc32.getValue();
	}

	public static void main(String[] args) {
		long i= 3980304869l;
		System.out.println((int)i);
		
	}

}
