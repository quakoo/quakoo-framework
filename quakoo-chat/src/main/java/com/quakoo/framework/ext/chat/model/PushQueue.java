package com.quakoo.framework.ext.chat.model;

import java.io.Serializable;

public class PushQueue implements Serializable {
	
	private static final long serialVersionUID = 3480362556113883565L;

	private long id;
	
	private long uid;
	
	private long mid;
	
	private int status;
	
	private long time;

	public long getUid() {
		return uid;
	}

	public void setUid(long uid) {
		this.uid = uid;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getMid() {
		return mid;
	}

	public void setMid(long mid) {
		this.mid = mid;
	}

	@Override
	public String toString() {
		return "PushQueue [id=" + id + ", uid=" + uid + ", mid=" + mid
				+ ", status=" + status + ", time=" + time + "]";
	}

}
