package com.quakoo.framework.ext.push.service.impl;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.Resource;

import com.quakoo.framework.ext.push.model.param.*;
import io.netty.channel.ChannelHandlerContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import com.google.common.collect.Maps;
import com.quakoo.framework.ext.push.context.handle.PushNioHandleContextHandle;
import com.quakoo.framework.ext.push.model.constant.Brand;
import com.quakoo.framework.ext.push.model.constant.Platform;
import com.quakoo.framework.ext.push.nio.ChannelUtils;
import com.quakoo.framework.ext.push.service.IosPushService;
import com.quakoo.framework.ext.push.service.PushNioConnectService;
import com.quakoo.framework.ext.push.service.PushUserService;


/**
 * 长连接处理类
 * class_name: PushNioConnectServiceImpl
 * package: com.quakoo.framework.ext.push.service.impl
 * creat_user: lihao
 * creat_date: 2019/1/30
 * creat_time: 14:59
 **/
public class PushNioConnectServiceImpl implements PushNioConnectService, InitializingBean {
	
	Logger logger = LoggerFactory.getLogger(PushNioConnectServiceImpl.class);
	
	@Resource
	private PushUserService pushUserService;
	
	@Resource
	private IosPushService iosPushService;
	
	private LinkedBlockingQueue<NioConnectQueueItem> queue = 
			new LinkedBlockingQueue<NioConnectQueueItem>();
	
	private final int threadNum = Runtime.getRuntime().availableProcessors() * 2 + 1;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		for(int i = 0; i < threadNum; i++) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					while(true) {
						NioConnectQueueItem queueItem = null;
						try {
							queueItem = queue.take();
							handle(queueItem);
						} catch (Exception e) {
							logger.error(e.getMessage(), e);
							if(null != queueItem) {
								ChannelHandlerContext ctx = queueItem.getCtx();
								ErrorResponse response = new ErrorResponse();
								response.setMsg("handle error!");
								ChannelUtils.write(ctx, response, false);
							}
						}
					}
				}
			}).start();
		}
	}

	/**
     * 接收到消息放入到队列
	 * method_name: handle
	 * params: [ctx, sessionRequest]
	 * return: void
	 * creat_user: lihao
	 * creat_date: 2019/1/30
	 * creat_time: 15:00
	 **/
	@Override
	public void handle(ChannelHandlerContext ctx, SessionRequest sessionRequest) {
		NioConnectQueueItem item = new NioConnectQueueItem();
		item.setCtx(ctx);
		item.setSessionRequest(sessionRequest);
		queue.add(item);
	}
	
	private void handle(NioConnectQueueItem queueItem) {
		ChannelHandlerContext ctx = queueItem.getCtx();
		SessionRequest sessionRequest = queueItem.getSessionRequest();
		String sessionId = sessionRequest.getSessionId();
		if(sessionRequest instanceof RegistRequest) { //处理注册请求
			long uid = ((RegistRequest) sessionRequest).getUid();
			int platform = ((RegistRequest) sessionRequest).getPlatform();
			int brand = ((RegistRequest) sessionRequest).getBrand();
			String phoneSessionId = ((RegistRequest) sessionRequest).getPhoneSessionId();
			String iosToken = ((RegistRequest) sessionRequest).getIosToken();
            String huaWeiToken = ((RegistRequest) sessionRequest).getHuaWeiToken();
            String meiZuPushId = ((RegistRequest) sessionRequest).getMeiZuPushId();
			try {
                boolean sign = pushUserService.registUserInfo(uid, platform, brand, phoneSessionId, iosToken, huaWeiToken, meiZuPushId);
                if(sign) {
                    if (platform == Platform.android && brand == Brand.common) {
                        NioUserLongConnection nioUserLongConnection = new NioUserLongConnection();
                        nioUserLongConnection.setUid(uid);
                        nioUserLongConnection.setPlatform(platform);
                        nioUserLongConnection.setBrand(brand);
                        nioUserLongConnection.setSessionId(phoneSessionId);
                        nioUserLongConnection.setActiveTime(System.currentTimeMillis());
                        Map<ChannelHandlerContext, NioUserLongConnection> map = Maps.newConcurrentMap();
                        map.put(ctx, nioUserLongConnection);
                        PushNioHandleContextHandle.connection_context.put(uid, map);

//                        Map<ChannelHandlerContext, NioUserLongConnection> map = PushNioHandleContextHandle.
//                                connection_context.get(uid);
//                        if (null == map) {
//                            map = Maps.newConcurrentMap();
//                            PushNioHandleContextHandle.connection_context.put(uid, map);
//                        }
//                        map.put(ctx, nioUserLongConnection);
                    }
                }
				RegistResponse response = new RegistResponse();
				response.setSessionId(sessionId);
				response.setSuccess(sign);
				ChannelUtils.write(ctx, response, false);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				RegistResponse response = new RegistResponse();
				response.setSessionId(sessionId);
				response.setSuccess(false);
				response.setErrMsg(e.getMessage());
				ChannelUtils.write(ctx, response, false);
			}
		} else if(sessionRequest instanceof LogoutRequest) { //处理登出请求
            long uid = ((LogoutRequest) sessionRequest).getUid();
            int platform = ((LogoutRequest) sessionRequest).getPlatform();
            int brand = ((LogoutRequest) sessionRequest).getBrand();
            String phoneSessionId = ((LogoutRequest) sessionRequest).getPhoneSessionId();
            try {
                boolean sign = pushUserService.logoutUserInfo(uid);
                if(sign) {
                    PushNioHandleContextHandle.connection_context.remove(uid);
                }
                LogoutResponse response = new LogoutResponse();
                response.setSessionId(sessionId);
                response.setSuccess(sign);
                ChannelUtils.write(ctx, response, false);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                LogoutResponse response = new LogoutResponse();
                response.setSessionId(sessionId);
                response.setSuccess(false);
                response.setErrMsg(e.getMessage());
                ChannelUtils.write(ctx, response, false);
            }
        } else if(sessionRequest instanceof PingRequest) { //处理心跳请求
			long uid = ((PingRequest) sessionRequest).getUid();
			int platform = ((PingRequest) sessionRequest).getPlatform();
			int brand = ((PingRequest) sessionRequest).getBrand();
			String phoneSessionId = ((PingRequest) sessionRequest).getPhoneSessionId();
			if(platform == Platform.android && brand == Brand.common) {
				NioUserLongConnection nioUserLongConnection = new NioUserLongConnection();
				nioUserLongConnection.setUid(uid);
				nioUserLongConnection.setPlatform(platform);
				nioUserLongConnection.setBrand(brand);
				nioUserLongConnection.setSessionId(phoneSessionId);
				nioUserLongConnection.setActiveTime(System.currentTimeMillis());
				Map<ChannelHandlerContext, NioUserLongConnection> map = PushNioHandleContextHandle.
						connection_context.get(uid);
				if(null != map) {
					map.put(ctx, nioUserLongConnection);
				} 
			}
			PongResponse response = new PongResponse();
			response.setSessionId(sessionId);
			response.setSuccess(true);
			ChannelUtils.write(ctx, response, false);
		} else if (sessionRequest instanceof IosClearBadgeRequest) { //处理清除IOS小红点请求
			long uid = ((IosClearBadgeRequest) sessionRequest).getUid();
			try {
				boolean sign = iosPushService.clearBadge(uid);
				IosClearBadgeResponse response = new IosClearBadgeResponse();
				response.setSessionId(sessionId);
				response.setSuccess(sign);
				ChannelUtils.write(ctx, response, false);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				IosClearBadgeResponse response = new IosClearBadgeResponse();
				response.setSessionId(sessionId);
				response.setSuccess(false);
				response.setErrMsg(e.getMessage());
				ChannelUtils.write(ctx, response, false);
			}
		}
	}

}
