package com.quakoo.framework.ext.chat.model.param.nio;

import io.netty.channel.ChannelHandlerContext;

public class NioSessionQueueItem {

	private ChannelHandlerContext ctx;
	
	private SessionRequest sessionRequest;
	
	public NioSessionQueueItem() {
		super();
	}

	public NioSessionQueueItem(ChannelHandlerContext ctx,
			SessionRequest sessionRequest) {
		super();
		this.ctx = ctx;
		this.sessionRequest = sessionRequest;
	}

	public ChannelHandlerContext getCtx() {
		return ctx;
	}

	public void setCtx(ChannelHandlerContext ctx) {
		this.ctx = ctx;
	}

	public SessionRequest getSessionRequest() {
		return sessionRequest;
	}

	public void setSessionRequest(SessionRequest sessionRequest) {
		this.sessionRequest = sessionRequest;
	}
	
}
