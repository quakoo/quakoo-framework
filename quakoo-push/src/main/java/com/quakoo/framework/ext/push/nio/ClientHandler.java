package com.quakoo.framework.ext.push.nio;


import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.framework.ext.push.model.constant.Brand;
import com.quakoo.framework.ext.push.model.constant.Platform;
import com.quakoo.framework.ext.push.model.param.ErrorResponse;
import com.quakoo.framework.ext.push.model.param.NioRequest;
import com.quakoo.framework.ext.push.model.param.NioResponse;
import com.quakoo.framework.ext.push.model.param.PayloadResponse;
import com.quakoo.framework.ext.push.model.param.PingRequest;
import com.quakoo.framework.ext.push.model.param.PongResponse;
import com.quakoo.framework.ext.push.model.param.RegistResponse;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.util.ReferenceCountUtil;

@Sharable
public class ClientHandler extends ChannelInboundHandlerAdapter {

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object obj)
			throws Exception {
		try {
			if(obj instanceof String) {
				String msg = String.valueOf(obj);
				NioResponse nioResponse = JsonUtils.fromJson(msg, NioResponse.class);
				int type = nioResponse.getType();
				if(type == NioResponse.type_regist) {
					RegistResponse registResponse = JsonUtils.fromJson(msg, RegistResponse.class);
					System.out.println(registResponse.toString());
				} else if (type == NioResponse.type_pong) {
					PongResponse pongResponse = JsonUtils.fromJson(msg, PongResponse.class);
					System.out.println(pongResponse.toString());
				} else if (type == NioResponse.type_payload) {
					PayloadResponse payloadResponse = JsonUtils.fromJson(msg, PayloadResponse.class);
					System.out.println(payloadResponse.toString());
				} else if(type == NioResponse.type_error) {
					ErrorResponse errorResponse = JsonUtils.fromJson(msg, ErrorResponse.class);
					System.out.println(errorResponse.toString());
				}
				
//				Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);
//				PingRequest pingRequest = new PingRequest();
//				pingRequest.setUid(2);
//				pingRequest.setType(NioRequest.type_ping);
//				pingRequest.setPlatform(Platform.android);
//				pingRequest.setSessionId(String.valueOf(System.currentTimeMillis()));
//				pingRequest.setPhoneSessionId(String.valueOf(2));
//				pingRequest.setBrand(Brand.common);
//				ChannelUtils.write(ctx, pingRequest, false);
			}
		} finally {
			ReferenceCountUtil.release(obj);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		cause.printStackTrace();
	}

}
