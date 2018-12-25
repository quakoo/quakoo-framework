package com.quakoo.baseFramework.redis;

public class RedisDecrParam {

	private String key;
	
	private Object attach;
	
	public RedisDecrParam() {
		super();
	}

	public RedisDecrParam(String key) {
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
		return "RedisDecrParam [" + (key != null ? "key=" + key : "") + "]";
	}
	
}
