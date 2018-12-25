package com.quakoo.framework.ext.chat.model;

import java.io.Serializable;

public class ManyChatQueue implements Serializable {

	private static final long serialVersionUID = -7976205791835148951L;

	private long uid;

	private long cgid;
	
	private long mid;
	
	private int status;
	
	private long time;

	public long getUid() {
		return uid;
	}

	public void setUid(long uid) {
		this.uid = uid;
	}

	public long getCgid() {
		return cgid;
	}

	public void setCgid(long cgid) {
		this.cgid = cgid;
	}

	public long getMid() {
		return mid;
	}

	public void setMid(long mid) {
		this.mid = mid;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "ManyChatQueue [uid=" + uid + ", cgid=" + cgid + ", mid=" + mid
				+ ", status=" + status + ", time=" + time + "]";
	}

}
