package com.quakoo.framework.ext.chat.model.param.nio;

import io.netty.channel.ChannelHandlerContext;

public class NioPromptQueueItem {
	
	private ChannelHandlerContext ctx;

	private long uid;
	
	private double lastPromptIndex;
	

	public NioPromptQueueItem() {
		super();
	}

	public NioPromptQueueItem(ChannelHandlerContext ctx, long uid,
			double lastPromptIndex) {
		super();
		this.ctx = ctx;
		this.uid = uid;
		this.lastPromptIndex = lastPromptIndex;
	}

	public ChannelHandlerContext getCtx() {
		return ctx;
	}

	public void setCtx(ChannelHandlerContext ctx) {
		this.ctx = ctx;
	}

	public long getUid() {
		return uid;
	}

	public void setUid(long uid) {
		this.uid = uid;
	}

	public double getLastPromptIndex() {
		return lastPromptIndex;
	}

	public void setLastPromptIndex(double lastPromptIndex) {
		this.lastPromptIndex = lastPromptIndex;
	}
	
}
