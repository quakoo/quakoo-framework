package com.quakoo.framework.ext.push.model.param;

import com.quakoo.framework.ext.push.model.PushMsg;

import java.io.Serializable;
import java.util.List;

public class InternalPushItem implements Serializable {

	private static final long serialVersionUID = 1L;

	private List<Long> uids;

	private PushMsg pushMsg;

	public List<Long> getUids() {
		return uids;
	}

	public void setUids(List<Long> uids) {
		this.uids = uids;
	}

    public PushMsg getPushMsg() {
        return pushMsg;
    }

    public void setPushMsg(PushMsg pushMsg) {
        this.pushMsg = pushMsg;
    }

    @Override
    public String toString() {
        return "InternalPushItem{" +
                "uids=" + uids +
                ", pushMsg=" + pushMsg +
                '}';
    }
}
