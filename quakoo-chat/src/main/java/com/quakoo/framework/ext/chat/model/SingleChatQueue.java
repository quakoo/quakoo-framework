package com.quakoo.framework.ext.chat.model;

import java.io.Serializable;

public class SingleChatQueue implements Serializable{

	private static final long serialVersionUID = -2162152044070052064L;

	private long uid;

	private long toUid;
	
	private long mid;
	
	private long time;
	
	private int status;
	
	public long getUid() {
		return uid;
	}

	public void setUid(long uid) {
		this.uid = uid;
	}

	public long getToUid() {
		return toUid;
	}

	public void setToUid(long toUid) {
		this.toUid = toUid;
	}

	public long getMid() {
		return mid;
	}

	public void setMid(long mid) {
		this.mid = mid;
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

	@Override
	public String toString() {
		return "SingleChatQueue [uid=" + uid + ", toUid=" + toUid + ", mid="
				+ mid + ", status=" + status + ", time=" + time + "]";
	}

}
