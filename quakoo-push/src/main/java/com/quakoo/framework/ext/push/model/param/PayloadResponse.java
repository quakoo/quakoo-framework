package com.quakoo.framework.ext.push.model.param;

import java.util.List;

import com.google.common.collect.Lists;
import com.quakoo.framework.ext.push.model.PushMsg;

public class PayloadResponse extends NioResponse {

    private List<PushMsg> payloads;

    public PayloadResponse() {
        super();
        super.type = type_payload;
        super.success = true;
    }

    public void setOne(PushMsg pushMsg) {
        payloads = Lists.newArrayList();
        payloads.add(pushMsg);
    }

    public List<PushMsg> getPayloads() {
        return payloads;
    }

    public void setPayloads(List<PushMsg> payloads) {
        this.payloads = payloads;
    }

    @Override
    public String toString() {
        return "PayloadResponse ["
                + (payloads != null ? "payloads=" + payloads + ", " : "")
                + "type=" + type + ", success=" + success + "]";
    }
	
}
