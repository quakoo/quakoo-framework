package com.quakoo.framework.ext.push.model.param;

public class IosClearBadgeResponse extends SessionResponse {

	public IosClearBadgeResponse() {
		super();
		this.type = type_ios_clear_badge;
	}

	@Override
	public String toString() {
		return "IosClearBadgeResponse ["
				+ (sessionId != null ? "sessionId=" + sessionId + ", " : "")
				+ (errMsg != null ? "errMsg=" + errMsg + ", " : "") + "type="
				+ type + ", success=" + success + "]";
	}
	
}
