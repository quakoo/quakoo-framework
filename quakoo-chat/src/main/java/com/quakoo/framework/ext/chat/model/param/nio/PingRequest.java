package com.quakoo.framework.ext.chat.model.param.nio;

public class PingRequest extends SessionRequest {

	private long uid;

	private double lastIndex;

	public PingRequest() {
		super();
		super.type = type_ping;
	}

	public PingRequest(long uid, double lastIndex) {
		super();
		super.type = type_ping;
		this.uid = uid;
		this.lastIndex = lastIndex;
	}

	public long getUid() {
		return uid;
	}

	public void setUid(long uid) {
		this.uid = uid;
	}

	public double getLastIndex() {
		return lastIndex;
	}

	public void setLastIndex(double lastIndex) {
		this.lastIndex = lastIndex;
	}
	
}
