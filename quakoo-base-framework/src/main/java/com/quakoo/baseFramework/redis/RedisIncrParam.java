package com.quakoo.baseFramework.redis;

public class RedisIncrParam {

	private String key;
	
	private Object attach;
	
	public RedisIncrParam() {
		super();
	}
	public RedisIncrParam(String key) {
		super();
		this.key = key;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	
	
	public Object getAttach() {
		return attach;
	}
	public void setAttach(Object attach) {
		this.attach = attach;
	}
	@Override
	public String toString() {
		return "RedisIncrParam [" + (key != null ? "key=" + key : "") + "]";
	}
	
	
}
