package com.quakoo.baseFramework.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * 
 * @author liyongbiao
 *
 */
public class HttpResult {

	private byte[] body;
	private int code;
	private Map<String, String> headers;
	private String charset;
	private InputStream inputStream;

	public String getHeader(String name) {
		return headers.get(name);
	}

	/**
	 * @return the body
	 */
	public byte[] getBody() {
		return body;
	}

	/**
	 * @param body
	 *            the body to set
	 */
	public void setBody(byte[] body) {
		this.body = body;
	}

	/**
	 * @return the code
	 */
	public int getCode() {
		return code;
	}

	/**
	 * @param code
	 *            the code to set
	 */
	public void setCode(int code) {
		this.code = code;
	}

	/**
	 * @return the headers
	 */
	public Map<String, String> getHeaders() {
		return headers;
	}

	/**
	 * @param headers
	 *            the headers to set
	 */
	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}
	
	public String getResult() {
			try {
				if(body==null||body.length==0){
					return null;
				}
				if(charset==null){
					charset= Charset.defaultCharset().name();
				}
				return new String(body,charset);
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	public void close() {
		try {
			inputStream.close();
		}catch (Exception e){

		}
	}
}
