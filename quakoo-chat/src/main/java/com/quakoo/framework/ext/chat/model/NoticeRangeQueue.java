package com.quakoo.framework.ext.chat.model;

import java.io.Serializable;

public class NoticeRangeQueue implements Serializable {

	private static final long serialVersionUID = 1208345248267410284L;

    private long authorId;
	
	private long mid;
	
	private String uids;
	
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

	public String getUids() {
		return uids;
	}

	public void setUids(String uids) {
		this.uids = uids;
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
		return "NoticeRangeQueue [authorId=" + authorId + ", mid=" + mid + ", "
				+ (uids != null ? "uids=" + uids + ", " : "") + "time=" + time
				+ ", status=" + status + "]";
	}
	
}
