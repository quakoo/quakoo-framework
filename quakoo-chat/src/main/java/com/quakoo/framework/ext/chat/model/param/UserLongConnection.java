package com.quakoo.framework.ext.chat.model.param;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.AsyncContext;

public class UserLongConnection {
	
	private long uid;
	private double lastMsgSort;
	private AsyncContext asyncContext;
	private long startTime;
	private AtomicBoolean sended = new AtomicBoolean(false);
	
	public UserLongConnection(long uid, double lastMsgSort,
			AsyncContext asyncContext, long startTime) {
		super();
		this.uid = uid;
		this.lastMsgSort = lastMsgSort;
		this.asyncContext = asyncContext;
		this.startTime = startTime;
	}
	
	public UserLongConnection() {
		super();
	}

	public boolean getAndSetSended(boolean sign){
		return this.sended.getAndSet(sign);
	}

	public boolean getSended() {
		return sended.get();
	}

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

	public AsyncContext getAsyncContext() {
		return asyncContext;
	}
	public void setAsyncContext(AsyncContext asyncContext) {
		this.asyncContext = asyncContext;
	}
	public long getStartTime() {
		return startTime;
	}
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	

	@Override
	public String toString() {
		return "UserLongConnection [uid="
				+ uid
				+ ", lastMsgSort="
				+ lastMsgSort
				+ ", "
				+ (asyncContext != null ? "asyncContext=" + asyncContext + ", "
						: "") + "startTime=" + startTime + ", sended=" + sended
				+ "]";
	}
}
