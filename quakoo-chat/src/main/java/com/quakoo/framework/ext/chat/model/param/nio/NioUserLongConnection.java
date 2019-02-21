package com.quakoo.framework.ext.chat.model.param.nio;


import java.text.DecimalFormat;

public class NioUserLongConnection {

    private DecimalFormat decimalFormat = new DecimalFormat("###################.###");

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
        return "{uid=" + uid + ", lastMsgSort=" + decimalFormat.format(lastMsgSort) + "}";
    }
}
