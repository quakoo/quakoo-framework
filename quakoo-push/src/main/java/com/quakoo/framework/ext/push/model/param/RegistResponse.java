package com.quakoo.framework.ext.push.model.param;

public class RegistResponse extends SessionResponse {

	public RegistResponse() {
		super();
		super.type = type_regist;
	}

	@Override
	public String toString() {
		return "RegistResponse ["
				+ (sessionId != null ? "sessionId=" + sessionId + ", " : "")
				+ (errMsg != null ? "errMsg=" + errMsg + ", " : "") + "type="
				+ type + ", success=" + success + "]";
	}
	
}
