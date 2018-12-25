package com.quakoo.framework.ext.chat.model.param.nio;


public class NioUserLongConnection {

	private long uid;
	
	private double lastMsgSort;
	
	private long activeTime;

	public long getUid() {
		return uid;
	}

	public void setUid(long uid) {
		this.uid = uid;
	}

	public double getLastMsgSort() {
		return lastMsgSort;
	}

	public void setLastMsgSort(double lastMsgSort) {
		this.lastMsgSort = lastMsgSort;
	}

	public long getActiveTime() {
		return activeTime;
	}

	public void setActiveTime(long activeTime) {
		this.activeTime = activeTime;
	}

	@Override
	public String toString() {
		return "NioUserLongConnection [uid=" + uid + ", lastMsgSort="
				+ lastMsgSort + ", activeTime=" + activeTime + "]";
	}

}
