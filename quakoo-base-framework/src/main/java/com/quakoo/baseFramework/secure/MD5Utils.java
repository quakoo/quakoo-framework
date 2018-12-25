package com.quakoo.baseFramework.secure;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

/**
 * md5生成
 * 
 * @author liyongbiao
 */
public class MD5Utils {

    private static final String  algorithm="MD5";


    public static byte[] md5(InputStream inputStream,int tempSize,long pos,long len) throws NoSuchAlgorithmException, IOException {
        return MessageDigestUtils.digest(algorithm,inputStream,tempSize,pos,len);
    }

    public static String md5ReStr(InputStream inputStream,int tempSize,long pos,long len) throws NoSuchAlgorithmException, IOException {
        return MessageDigestUtils.digestReStr(algorithm,inputStream,tempSize,pos,len);
    }

    /**
     * 计算数据的md5返回byte数组
     */
    public static byte[] md5(byte[] data) throws NoSuchAlgorithmException {
        return MessageDigestUtils.digest(algorithm, data);
    }

    /**
     * 计算数据的md5返回字符串
     */
    public static String md5ReStr(byte[] data) throws NoSuchAlgorithmException{
        return MessageDigestUtils.digestReStr(algorithm,data);
    }


    public static byte[] md5(byte[] b, int off, int len) throws NoSuchAlgorithmException {
        return MessageDigestUtils.digest(algorithm,b,off,len);
    }

    public static String md5ReStr(byte[] b, int off, int len) throws NoSuchAlgorithmException {
        return MessageDigestUtils.digestReStr(algorithm, b, off, len);
    }
    
    public static void main(String[] fwe) throws NoSuchAlgorithmException{
    		System.out.println(md5ReStr("我去".getBytes()));
    }
}
