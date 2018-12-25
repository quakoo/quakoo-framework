package com.quakoo.baseFramework.secure;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by 136249 on 2015/5/6.
 */
public class MessageDigestUtils {

    public static byte[] digest(String algorithm,InputStream inputStream,int tempSize,long pos,long len) throws NoSuchAlgorithmException, IOException {
        if(tempSize<=0){
            tempSize=1024;
        }
        MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
        DigestInputStream dis = new DigestInputStream(inputStream, messageDigest);

        if (len < 0) {
            throw new IndexOutOfBoundsException();
        }
        if(pos>0){
            long skip= inputStream.skip(pos);
            if(skip<pos){
                throw new RuntimeException("skip to pos error!,pos:"+pos+",skiped:"+skip);
            }
        }

        byte[] tmp = new byte[tempSize];
        int n = 0;
        while (n < len) {
            int readSize=tempSize;
            if(readSize>(len-n)){
                readSize= (int) (len-n);
            }
            int count = dis.read(tmp, 0, readSize);
            if (count < 0)
                throw new EOFException();
            n += count;
        }

        return messageDigest.digest();
    }

    public static String digestReStr(String algorithm,InputStream inputStream,int tempSize,long pos,long len) throws NoSuchAlgorithmException, IOException {
        byte[] digestBytes = digest(algorithm,inputStream, tempSize, pos, len);
        return Hex.bytesToHexString(digestBytes);
    }

    public static byte[] digest(String algorithm,byte[] data) throws NoSuchAlgorithmException  {
        MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
        return messageDigest.digest(data);
    }


    public static String digestReStr(String algorithm,byte[] data) throws NoSuchAlgorithmException {
        byte[] digestBytes = digest(algorithm,data);
        return Hex.bytesToHexString(digestBytes);
    }

    public static byte[] digest(String algorithm,byte[] b, int off, int len) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(b, off, len);
        return md.digest();
    }

    public static String digestReStr(String algorithm,byte[] b, int off, int len) throws NoSuchAlgorithmException {
        return Hex.bytesToHexString(digest(algorithm, b, off, len));
    }
}
