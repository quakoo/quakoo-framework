package com.quakoo.framework.ext.push.service;


import com.quakoo.framework.ext.push.model.param.SessionRequest;

import io.netty.channel.ChannelHandlerContext;

public interface PushNioConnectService {

	public void handle(ChannelHandlerContext ctx, SessionRequest sessionRequest);
	
}
