package com.quakoo.framework.ext.chat.model.param.nio;

public class PongResponse extends SessionResponse {

	public PongResponse() {
		super();
		this.type = type_pong;
	}

	@Override
	public String toString() {
		return "PongResponse ["
				+ (sessionId != null ? "sessionId=" + sessionId + ", " : "")
				+ (errMsg != null ? "errMsg=" + errMsg + ", " : "") + "type="
				+ type + ", success=" + success + "]";
	}
	
}
