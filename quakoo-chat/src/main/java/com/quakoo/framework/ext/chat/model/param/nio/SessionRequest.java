package com.quakoo.framework.ext.chat.model.param.nio;

public class SessionRequest extends NioRequest {

	protected String sessionId;

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

}
