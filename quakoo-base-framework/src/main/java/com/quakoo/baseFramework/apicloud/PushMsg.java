package com.quakoo.baseFramework.apicloud;

public class PushMsg {

	private int type;
	private Object message;
	
	
	public PushMsg(int type, Object message) {
		super();
		this.type = type;
		this.message = message;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public Object getMessage() {
		return message;
	}
	public void setMessage(Object message) {
		this.message = message;
	}
	
	
}
