package com.quakoo.framework.ext.push.model;

import java.io.Serializable;

public class PushHandleAllQueue implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private long id; // #b# @ak@ ^nn^
	
	private long payloadId; // #b# ^nn^
	
	private long time; // #b# ^nn^

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getPayloadId() {
		return payloadId;
	}

	public void setPayloadId(long payloadId) {
		this.payloadId = payloadId;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

    @Override
	public String toString() {
		return "PushHandleAllQueue [id=" + id + ", payloadId=" + payloadId
				+ ", time=" + time + "]";
	}

}
