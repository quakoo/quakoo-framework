package com.quakoo.baseFramework.util;

/**
 * Created by 136249 on 2015/3/17.
 */
public class ArrayUtils {

    public static byte[] mergeBytes(byte[] bytes1,byte[] bytes2){
        byte[] bytes=new byte[bytes1.length+bytes2.length];
        for(int i=0;i<bytes1.length;i++){
            bytes[i]=bytes1[i];
        }
        for(int i=0;i<bytes2.length;i++){
            bytes[i+bytes1.length]=bytes2[i];
        }
        return bytes;
    }

    public static void main(String[] few){
        System.out.print(new String(mergeBytes("444444444444444444446".getBytes(),"fwe".getBytes())));
    }
}
