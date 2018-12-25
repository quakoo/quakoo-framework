package com.quakoo.baseFramework.secure;

import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * 
 * @author LiYongbiao1
 *
 */
public class ARC4Util {
	
	public static byte[] encode(byte[] key,byte[] data){
		try {
			Cipher cipher=Cipher.getInstance("RC4");
			SecretKeySpec secretKeySpec=new SecretKeySpec(key, "RC4");
			cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
			return cipher.update(data);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static byte[] decode(byte[] key,byte[] data){
		try {
			Cipher cipher=Cipher.getInstance("RC4");
			SecretKeySpec secretKeySpec=new SecretKeySpec(key, "RC4");
			cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
			return cipher.update(data);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param args
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 */
	public static void main(String[] args) throws Exception {
		
	}

}
