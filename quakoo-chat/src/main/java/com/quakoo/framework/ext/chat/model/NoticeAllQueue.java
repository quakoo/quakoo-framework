package com.quakoo.framework.ext.chat.model;

import java.io.Serializable;

public class NoticeAllQueue implements Serializable {

	private static final long serialVersionUID = 3874769790572041157L;
	
	private long authorId;
	
	private long mid;
	
	private long time;
	
	private int status;

	public long getAuthorId() {
		return authorId;
	}

	public void setAuthorId(long authorId) {
		this.authorId = authorId;
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
		return "NoticeAllQueue [authorId=" + authorId + ", mid=" + mid
				+ ", time=" + time + ", status=" + status + "]";
	}

}
