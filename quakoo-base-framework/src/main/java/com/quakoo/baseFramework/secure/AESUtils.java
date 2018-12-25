package com.quakoo.baseFramework.secure;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import com.quakoo.baseFramework.util.ByteUtil;
import com.quakoo.baseFramework.util.ByteUtil;

/**
 * @author LiYongbiao1
 */
public class AESUtils {

    private static byte[] getKey256(String key) throws NoSuchAlgorithmException {
        ByteBuffer bb = ByteBuffer.allocate(32);
        byte[] md5Byte = MD5Utils.md5(key.getBytes());
        byte[] sha1Byte = Sha1Utils.sha1(key.getBytes());
        bb.put(md5Byte);
        bb.put(sha1Byte, 0, 16);
        return bb.array();
    }

    /**
     * 加密数据
     */
    public static byte[] encrypt(byte[] src, String key) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        SecretKeySpec secureKey = new SecretKeySpec(getKey256(key), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secureKey);
        return cipher.doFinal(src);
    }

    /**
     * 解密数据
     */
    public static byte[] decrypt(byte[] src, String key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        SecretKeySpec securekey = new SecretKeySpec(getKey256(key), "AES");
        cipher.init(Cipher.DECRYPT_MODE, securekey);
        return cipher.doFinal(src);
    }

    /**
     * 加密文件数据用（NoPadding可以保持对称性）
     * 
     * @throws Exception
     */
    public static byte[] encryptNoPaddingData(byte[] src, String key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        SecretKeySpec securekey = new SecretKeySpec(getKey256(key), "AES");
        int remainder = src.length % 16;
        int paddingLength = (remainder == 0) ? 0 : 16 - remainder;
        if (paddingLength > 0) {
            src = ByteBuffer.allocate(src.length + paddingLength).put(src).array();
        }
        cipher.init(Cipher.ENCRYPT_MODE, securekey);
        return cipher.doFinal(src);
    }

    /**
     * 解密文件数据用
     * 
     * @param src
     *            加密后的数据
     * @param key
     *            解密key
     * @param len
     *            加密前文件长度
     * @throws Exception
     */
    public static byte[] decryptNoPaddingData(byte[] src, String key, int len) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        SecretKeySpec securekey = new SecretKeySpec(getKey256(key), "AES");
        cipher.init(Cipher.DECRYPT_MODE, securekey);
        byte[] data = cipher.doFinal(src);
        int remainder = len % 16;
        int paddingLength = (remainder == 0) ? 0 : 16 - remainder;
        if (paddingLength > 0) {
            byte[] tmp = new byte[len];
            System.arraycopy(data, 0, tmp, 0, len);
            data = tmp;
        }
        return data;
    }

    /**
     * 解密需要的文件数据
     * 
     * @throws Exception
     */
    public static byte[] decryptNoPadding(byte[] src, String key, int off, int len) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        SecretKeySpec securekey = new SecretKeySpec(getKey256(key), "AES");
        cipher.init(Cipher.DECRYPT_MODE, securekey);
        return cipher.doFinal(src, off, len);
    }

    public static void main(String[] fwef) throws Exception {
        String key = "quanmingpaihangbang";
        String value = "161026";
        // System.out.println(new
        // String(decryptNoPaddingData(encryptNoPaddingData(value.getBytes(),
        // key), key,
        // value.getBytes().length)));
        // System.out.println(new String(decrypt(encrypt(value.getBytes(), key),
        // key)));

        System.out.println(Hex.bytesToHexString(AESUtils.encrypt(ByteUtil.putLong(170073), key)));

    }

}
