package com.quakoo.framework.ext.push.model.param;

import io.netty.channel.ChannelHandlerContext;

public class NioConnectQueueItem {

	private ChannelHandlerContext ctx;
	
	private SessionRequest sessionRequest;
	
	public NioConnectQueueItem() {
		super();
	}

	public NioConnectQueueItem(ChannelHandlerContext ctx,
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
