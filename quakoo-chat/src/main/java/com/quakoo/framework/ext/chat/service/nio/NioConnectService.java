package com.quakoo.framework.ext.chat.service.nio;



import com.quakoo.framework.ext.chat.model.param.nio.SessionRequest;

import io.netty.channel.ChannelHandlerContext;

public interface NioConnectService {

	public void handle(ChannelHandlerContext ctx, SessionRequest sessionRequest);
	
//	public void connect(ChannelHandlerContext ctx, ConnectRequest connectRequest); 
	
//	public void sessionRequest(ChannelHandlerContext ctx, SessionRequest sessionRequest);
	
}
