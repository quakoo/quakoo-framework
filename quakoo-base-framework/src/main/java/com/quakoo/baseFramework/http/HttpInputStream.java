package com.quakoo.baseFramework.http;

import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by 136249 on 2015/3/5.
 */
public class HttpInputStream extends InputStream {

    private InputStream realStream;
    private HttpRequestBase method;

    public HttpInputStream(InputStream realStream, HttpRequestBase method) {
        this.realStream = realStream;
        this.method = method;
    }

    @Override
    public int read() throws IOException {
        return realStream.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return realStream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return realStream.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return realStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return realStream.available();
    }

    @Override
    public void close() throws IOException {
        try {
            realStream.close();
        }catch (Exception e){

        }
        try {
            if(method!=null) {
                method.abort();
            }
        }catch (Exception e){

        }
    }

    @Override
    public synchronized void mark(int readlimit) {
        realStream.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        realStream.reset();
    }

    @Override
    public boolean markSupported() {
        return realStream.markSupported();
    }
}
