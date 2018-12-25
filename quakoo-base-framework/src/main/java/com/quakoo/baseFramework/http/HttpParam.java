package com.quakoo.baseFramework.http;

import java.nio.charset.Charset;

/**
 * Created by 136249 on 2015/3/30.
 */
public class HttpParam {

    private String defaultCharSet = Charset.defaultCharset().name();
    /**
     * socket sotime
     */
    private int soTime=2000;
    /**
     * connect timeout
     */
    private int connectioneTime=1000;
    /**
     * retry time
     */
    private int reTryTimes=3;
    /**
     * 是否记录时间的log
     */
    private boolean logTime=true;
    /**
     * 是否记录返回结果的log
     */
    private boolean logResult=false;
    /**
     * 是否返回流
     */
    private boolean returnStream=false;


    /**
     * 请求的字符编码
     */
    private String reqCharSet=defaultCharSet;
    /**
     * 的字符编码
     */
    private String resCharSet=defaultCharSet;
    /**
     * 默认对url进行encode
     */
    private boolean urlEncode=true;

    /**
     * 默认对postForm进行encode
     */
    private boolean postFromEncode=true;



    public HttpParam(int soTime, int connectioneTime, int reTryTimes) {
        this.soTime = soTime;
        this.connectioneTime = connectioneTime;
        this.reTryTimes = reTryTimes;
    }

    public HttpParam(int soTime, int connectioneTime, int reTryTimes, boolean logTime, boolean logResult, boolean returnStream) {
        this.soTime = soTime;
        this.connectioneTime = connectioneTime;
        this.reTryTimes = reTryTimes;
        this.logTime = logTime;
        this.logResult = logResult;
        this.returnStream = returnStream;
    }

    public HttpParam(int soTime, int connectioneTime, int reTryTimes, boolean logTime, boolean logResult, boolean returnStream, String reqCharSet, String resCharSet, boolean urlEncode) {
        this.soTime = soTime;
        this.connectioneTime = connectioneTime;
        this.reTryTimes = reTryTimes;
        this.logTime = logTime;
        this.logResult = logResult;
        this.returnStream = returnStream;
        this.reqCharSet = reqCharSet;
        this.resCharSet = resCharSet;
        this.urlEncode = urlEncode;
    }

    public int getSoTime() {
        return soTime;
    }

    public void setSoTime(int soTime) {
        this.soTime = soTime;
    }

    public int getConnectioneTime() {
        return connectioneTime;
    }

    public void setConnectioneTime(int connectioneTime) {
        this.connectioneTime = connectioneTime;
    }

    public int getReTryTimes() {
        return reTryTimes;
    }

    public void setReTryTimes(int reTryTimes) {
        this.reTryTimes = reTryTimes;
    }

    public boolean isLogTime() {
        return logTime;
    }

    public void setLogTime(boolean logTime) {
        this.logTime = logTime;
    }

    public boolean isLogResult() {
        return logResult;
    }

    public void setLogResult(boolean logResult) {
        this.logResult = logResult;
    }

    public boolean isReturnStream() {
        return returnStream;
    }

    public void setReturnStream(boolean returnStream) {
        this.returnStream = returnStream;
    }


    public String getReqCharSet() {
        return reqCharSet;
    }

    public void setReqCharSet(String reqCharSet) {
        this.reqCharSet = reqCharSet;
    }

    public String getResCharSet() {
        return resCharSet;
    }

    public void setResCharSet(String resCharSet) {
        this.resCharSet = resCharSet;
    }

    public boolean isUrlEncode() {
        return urlEncode;
    }

    public void setUrlEncode(boolean urlEncode) {
        this.urlEncode = urlEncode;
    }

    public boolean isPostFromEncode() {
        return postFromEncode;
    }

    public void setPostFromEncode(boolean postFromEncode) {
        this.postFromEncode = postFromEncode;
    }



    @Override
	public String toString() {
		return "HttpParam [soTime=" + soTime + ", connectioneTime="
				+ connectioneTime + ", reTryTimes=" + reTryTimes + ", logTime="
				+ logTime + ", logResult=" + logResult + ", returnStream="
				+ returnStream + ", reqCharSet=" + reqCharSet + ", resCharSet="
				+ resCharSet + ", urlEncode=" + urlEncode + ", postFromEncode="
				+ postFromEncode + "]";
	}
    
    
}
