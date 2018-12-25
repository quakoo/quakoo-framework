package com.quakoo.baseFramework.redis;

import java.util.List;

public class RedisAddSetParam {
	
	private String key;
	
	private List<Object> members;
	
	public RedisAddSetParam() {
		super();
	}

	public RedisAddSetParam(String key, List<Object> members) {
		super();
		this.key = key;
		this.members = members;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public List<Object> getMembers() {
		return members;
	}

	public void setMembers(List<Object> members) {
		this.members = members;
	}
	
}
