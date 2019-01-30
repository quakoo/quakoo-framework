package com.quakoo.framework.ext.push.nio;

import javax.annotation.Resource;

import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.framework.ext.push.model.param.*;
import com.quakoo.framework.ext.push.service.PushNioConnectService;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.util.ReferenceCountUtil;

/**
 * 长连接处理类
 * class_name: ConnectHandler
 * package: com.quakoo.framework.ext.chat.nio
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 17:08
 **/
@Sharable
public class PushConnectHandler extends ChannelInboundHandlerAdapter {
	
	@Resource
	private PushNioConnectService pushNioConnectService;
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object obj)
			throws Exception {
		try {
			if(obj instanceof String){
				String msg = String.valueOf(obj);
				NioRequest nioRequest = JsonUtils.fromJson(msg, NioRequest.class);
				int type = nioRequest.getType();
				if(type == NioRequest.type_regist) {
					RegistRequest registRequest = JsonUtils.fromJson(msg, RegistRequest.class);
                    pushNioConnectService.handle(ctx, registRequest); //注册
				} else if(type == NioRequest.type_ping) {
					PingRequest pingRequest = JsonUtils.fromJson(msg, PingRequest.class);
                    pushNioConnectService.handle(ctx, pingRequest); //心跳
				} else if(type == NioRequest.type_ios_clear_badge) {
					IosClearBadgeRequest iosClearBadgeRequest = JsonUtils.fromJson(msg, IosClearBadgeRequest.class);
                    pushNioConnectService.handle(ctx, iosClearBadgeRequest); //IOS清除小红点
				} else if(type == NioRequest.type_logout) {
                    LogoutRequest logoutRequest = JsonUtils.fromJson(msg, LogoutRequest.class);
                    pushNioConnectService.handle(ctx, logoutRequest); //退出
                } else {
                    ChannelUtils.write(ctx, new ErrorResponse("receive msg type is not support!"), true);
                }
			} else {
				ChannelUtils.write(ctx, new ErrorResponse("receive msg is not string!"), true);
			}
		} finally {
			ReferenceCountUtil.release(obj);
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		cause.printStackTrace();
	}

	public static void main(String[] args) {
	}
}
