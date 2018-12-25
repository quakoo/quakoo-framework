package com.quakoo.framework.ext.chat.nio;

import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.framework.ext.chat.model.back.ConnectBack;
import com.quakoo.framework.ext.chat.model.param.nio.ConnectResponse;
import com.quakoo.framework.ext.chat.model.param.nio.ErrorResponse;
import com.quakoo.framework.ext.chat.model.param.nio.NioResponse;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.util.ReferenceCountUtil;

@Sharable
public class ClientHandler extends ChannelInboundHandlerAdapter {

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("--- server close this channel ---");
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object obj)
			throws Exception {
		try {
			if(obj instanceof String) {
				String msg = String.valueOf(obj);
				NioResponse nioResponse = JsonUtils.fromJson(msg, NioResponse.class);
				boolean success = nioResponse.isSuccess();
				if(success) {
					ConnectResponse connectResponse = JsonUtils.fromJson(msg, ConnectResponse.class);
					ConnectBack result = connectResponse.getResult();
					System.out.println(msg);
				} else {
					ErrorResponse errorResponse = JsonUtils.fromJson(msg, ErrorResponse.class);
					System.out.println(errorResponse.toString());
				}
			}
		} finally {
			ReferenceCountUtil.release(obj);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		cause.printStackTrace();
		System.out.println("error!!!!");
	}

}
