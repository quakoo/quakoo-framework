package com.quakoo.baseFramework.secure;

import redis.clients.util.SafeEncoder;

import java.io.IOException;

/**
 * 
 * @author LiYongbiao1
 *
 */
public class Base64Util {

	public static String encode(byte[] data) {
		if (data == null)
			return null;
		return (new sun.misc.BASE64Encoder()).encode(data);
	}
	public static byte[]  decode(String s) {
		if (s == null)
			return null;
		try {
			return (new sun.misc.BASE64Decoder()).decodeBuffer(s);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String createBASE64(String s) {
		if (s == null)
			return null;
		return (new sun.misc.BASE64Encoder()).encode(SafeEncoder.encode(s));
	}

	// 将 BASE64 编码的字符串 s 进行解码
	public static String getFromBASE64(String s) {
		if (s == null)
			return null;
		sun.misc.BASE64Decoder decoder = new sun.misc.BASE64Decoder();
		try {
			byte[] b = decoder.decodeBuffer(s);
			return SafeEncoder.encode(b);
		} catch (Exception e) {
			return null;
		}
	}
	
	public static String getBASE64(String s,int size) {
		if (s == null)
			return null;
		String ss= (new sun.misc.BASE64Encoder()).encode(SafeEncoder.encode(s));
		if(ss.length()>size){
			ss=ss.substring(0,size);
		}
		return ss;
	}
	
	
	public static void main(String[] sdg){
		
	}
}
