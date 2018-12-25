package com.quakoo.framework.ext.push.model.param;

import java.io.Serializable;
import java.util.List;

import com.quakoo.framework.ext.push.model.Payload;

public class InternalPushItem implements Serializable {

	private static final long serialVersionUID = 1L;

	private List<Long> uids;
	
	private Payload payload;

	public List<Long> getUids() {
		return uids;
	}

	public void setUids(List<Long> uids) {
		this.uids = uids;
	}

	public Payload getPayload() {
		return payload;
	}

	public void setPayload(Payload payload) {
		this.payload = payload;
	}

	@Override
	public String toString() {
		return "InternalPushItem ["
				+ (uids != null ? "uids=" + uids + ", " : "")
				+ (payload != null ? "payload=" + payload : "") + "]";
	}
	
}
