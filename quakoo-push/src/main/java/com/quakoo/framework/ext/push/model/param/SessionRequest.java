package com.quakoo.framework.ext.push.model.param;

public class SessionRequest extends NioRequest {

	protected String sessionId;

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	
}
