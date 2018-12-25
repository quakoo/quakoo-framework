package com.quakoo.baseFramework.util;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.quakoo.baseFramework.exception.ReadFullyErrorException;

/**
 * Created by xinql on 2015/2/26.
 */
public class StreamUtils {

    public static  void readFully(InputStream in,byte b[], int off, int len) throws IOException {
        if (len < 0)
            throw new IndexOutOfBoundsException();
        int n = 0;
        while (n < len) {
            int count = in.read(b, off + n, len - n);
            if (count < 0)
                throw new ReadFullyErrorException();
            n += count;
        }
    }

    public static  void  copyInputStream(InputStream inputStream,OutputStream outputStream,int bufferSize) throws IOException {
        org.apache.commons.io.IOUtils.copyLarge(inputStream, outputStream, new byte[bufferSize]);
    }

    public static  void  copyInputStream(InputStream inputStream,OutputStream outputStream,long offset,long length,int bufferSize) throws IOException {
        org.apache.commons.io.IOUtils.copyLarge(inputStream, outputStream, offset, length,new byte[bufferSize]);
    }


}
