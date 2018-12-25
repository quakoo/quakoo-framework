package com.quakoo.baseFramework.secure;


import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

public class Sha1Utils {

    private static final String  algorithm="sha1";


    public static byte[] sha1(InputStream inputStream,int tempSize,long pos,long len) throws NoSuchAlgorithmException, IOException {
        return MessageDigestUtils.digest(algorithm, inputStream, tempSize, pos, len);
    }

    public static String sha1ReStr(InputStream inputStream,int tempSize,long pos,long len) throws NoSuchAlgorithmException, IOException {
        return MessageDigestUtils.digestReStr(algorithm,inputStream,tempSize,pos,len);
    }

    public static byte[] sha1(byte[] data) throws NoSuchAlgorithmException {
        return MessageDigestUtils.digest(algorithm, data);
    }

    public static String sha1ReStr(byte[] data) throws NoSuchAlgorithmException{
        return MessageDigestUtils.digestReStr(algorithm,data);
    }


    public static byte[] sha1(byte[] b, int off, int len) throws NoSuchAlgorithmException {
        return MessageDigestUtils.digest(algorithm,b,off,len);
    }

    public static String sha1ReStr(byte[] b, int off, int len) throws NoSuchAlgorithmException {
        return MessageDigestUtils.digestReStr(algorithm, b, off, len);
    }



}
