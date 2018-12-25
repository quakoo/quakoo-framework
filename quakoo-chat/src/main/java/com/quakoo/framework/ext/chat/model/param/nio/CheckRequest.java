package com.quakoo.framework.ext.chat.model.param.nio;

public class CheckRequest extends SessionRequest {

	private long uid;
	
	private String clientId;

	public long getUid() {
		return uid;
	}

	public void setUid(long uid) {
		this.uid = uid;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	
}
