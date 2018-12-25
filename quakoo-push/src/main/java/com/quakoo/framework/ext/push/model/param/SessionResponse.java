package com.quakoo.framework.ext.push.model.param;

public class SessionResponse extends NioResponse {

	protected String sessionId;
	
	protected String errMsg;

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getErrMsg() {
		return errMsg;
	}

	public void setErrMsg(String errMsg) {
		this.errMsg = errMsg;
	}
	
}
