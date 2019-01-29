package com.quakoo.framework.ext.chat.nio;


import javax.annotation.Resource;

import com.mongodb.util.JSON;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.framework.ext.chat.context.handle.nio.NioHandleContextHandle;
import com.quakoo.framework.ext.chat.model.param.nio.*;
import com.quakoo.framework.ext.chat.service.ext.TokenCheckService;
import com.quakoo.framework.ext.chat.service.nio.NioConnectService;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 长连接处理类
 * class_name: ConnectHandler
 * package: com.quakoo.framework.ext.chat.nio
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 17:08
 **/
@Sharable
public class ConnectHandler extends ChannelInboundHandlerAdapter {

    private Logger logger = LoggerFactory.getLogger(ConnectHandler.class);

	@Resource
	private NioConnectService nioConnectService;

	@Resource
	private TokenCheckService tokenCheckService;

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		NioHandleContextHandle.connection_context.remove(ctx);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object obj) throws Exception {
		try {
			if(obj instanceof String){
				String msg = String.valueOf(obj);
				NioRequest nioRequest = JsonUtils.fromJson(msg, NioRequest.class);
				int type = nioRequest.getType();
				String token = nioRequest.getToken();
				boolean sign = tokenCheckService.check(token);
				if(!sign) {
                    AgainLoginResponse againLoginResponse = new AgainLoginResponse();
                    ChannelUtils.write(ctx, againLoginResponse, true);
                    return;
                }

				if(type == NioRequest.type_ping) { //心跳
					PingRequest pingRequest = JsonUtils.fromJson(msg, PingRequest.class);
					nioConnectService.handle(ctx, pingRequest);
				} else if(type == NioRequest.type_chat) { //聊天消息
					ChatRequest chatRequest = JsonUtils.fromJson(msg, ChatRequest.class);
					nioConnectService.handle(ctx, chatRequest);
				} else if(type == NioRequest.type_delete) { //删除消息
					DeleteRequest deleteRequest = JsonUtils.fromJson(msg, DeleteRequest.class);
					nioConnectService.handle(ctx, deleteRequest);
				} else if(type == NioRequest.type_check) { //检测消息
					CheckRequest checkRequest = JsonUtils.fromJson(msg, CheckRequest.class);
					nioConnectService.handle(ctx, checkRequest);
				} else if(type == NioRequest.type_recall) { //撤回消息
                    RecallRequest recallRequest = JsonUtils.fromJson(msg, RecallRequest.class);
                    nioConnectService.handle(ctx, recallRequest);
                } else if(type == NioRequest.type_other_chat) { //其他类型消息
				    logger.error("======= other_chat");
                    OtherChatRequest otherChatRequest = JsonUtils.fromJson(msg, OtherChatRequest.class);
                    nioConnectService.handle(ctx, otherChatRequest);
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
		NioHandleContextHandle.connection_context.remove(ctx);
        ctx.close();
	}

}
