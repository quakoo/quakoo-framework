package com.quakoo.framework.ext.push.model.param;

import java.util.List;

import com.google.common.collect.Lists;
import com.quakoo.framework.ext.push.model.Payload;

public class PayloadResponse extends NioResponse {

	private List<Payload> payloads;
	
	public PayloadResponse() {
		super();
		super.type = type_payload;
		super.success = true;
	}
	
	public void setOne(Payload payload) {
		payloads = Lists.newArrayList();
		payloads.add(payload);
	}
	
	public List<Payload> getPayloads() {
		return payloads;
	}

	public void setPayloads(List<Payload> payloads) {
		this.payloads = payloads;
	}

	@Override
	public String toString() {
		return "PayloadResponse ["
				+ (payloads != null ? "payloads=" + payloads + ", " : "")
				+ "type=" + type + ", success=" + success + "]";
	}
	
}
