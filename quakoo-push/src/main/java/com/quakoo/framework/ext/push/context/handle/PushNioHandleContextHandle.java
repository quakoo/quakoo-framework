package com.quakoo.framework.ext.push.context.handle;


import com.quakoo.framework.ext.push.model.PushMsg;
import io.netty.channel.ChannelHandlerContext;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.framework.ext.push.model.param.InternalPushItem;
import com.quakoo.framework.ext.push.model.param.NioUserLongConnection;
import com.quakoo.framework.ext.push.model.param.PayloadResponse;
import com.quakoo.framework.ext.push.nio.ChannelUtils;

/**
 * 长连接处理上下文
 * class_name: PushNioHandleContextHandle
 * package: com.quakoo.framework.ext.push.context.handle
 * creat_user: lihao
 * creat_date: 2019/1/30
 * creat_time: 11:16
 **/
public abstract class PushNioHandleContextHandle extends PushBaseContextHandle {
	
	Logger logger = LoggerFactory.getLogger(PushNioHandleContextHandle.class);
	
	public static volatile Map<Long, Map<ChannelHandlerContext, NioUserLongConnection>> connection_context  = 
			Maps.newConcurrentMap();
	
	public static volatile LinkedBlockingQueue<InternalPushItem> push_queue = new LinkedBlockingQueue<InternalPushItem>(); //推送消息的队列

	public static final long time_out = 1000 * 60 * 2;
	
	private final int threadNum = Runtime.getRuntime().availableProcessors() * 2 + 1;
	
	protected abstract void startConnectBootstrap();
	
	@Override
	public void afterPropertiesSet() throws Exception {
		startConnectBootstrap();
		for(int i = 0; i < threadNum; i++) {
			Thread pusher = new Thread(new Pusher());
			pusher.start();
		}
		Thread cleaner = new Thread(new Cleaner());
		cleaner.start();
	}

	/**
     * 推送线程
	 * class_name: PushNioHandleContextHandle
	 * package: com.quakoo.framework.ext.push.context.handle
	 * creat_user: lihao
	 * creat_date: 2019/1/30
	 * creat_time: 11:17
	 **/
	class Pusher implements Runnable {
		@Override
		public void run() {
			while(true) {
				InternalPushItem pushItem = null;
				try {
					pushItem = push_queue.take(); //从队列里获取待推送的消息
					List<Long> uids = pushItem.getUids();
					PushMsg pushMsg = pushItem.getPushMsg();
					PayloadResponse response = new PayloadResponse();
					response.setOne(pushMsg);
					for(long uid : uids) {
						Map<ChannelHandlerContext, NioUserLongConnection> map = connection_context.get(uid); //从用户推送字典里获取这个推送的长连接
						if(null != map && map.size() > 0) {
							for(ChannelHandlerContext ctx : map.keySet()) {
								ChannelUtils.write(ctx, response, false); //发送通知
							}
						}
					}
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
	}

	/**
     * 清理线程
	 * class_name: PushNioHandleContextHandle
	 * package: com.quakoo.framework.ext.push.context.handle
	 * creat_user: lihao
	 * creat_date: 2019/1/30
	 * creat_time: 11:17
	 **/
	class Cleaner implements Runnable {
		@Override
		public void run() {
			while(true) {
				Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
				for(Iterator<Entry<Long, Map<ChannelHandlerContext, NioUserLongConnection>>> it =
						connection_context.entrySet().iterator(); it.hasNext();){
					Entry<Long, Map<ChannelHandlerContext, NioUserLongConnection>> entry = it.next();
					Map<ChannelHandlerContext, NioUserLongConnection> connections = entry.getValue();
					for(Iterator<Entry<ChannelHandlerContext, NioUserLongConnection>> iterator = 
							connections.entrySet().iterator(); iterator.hasNext();){
						Entry<ChannelHandlerContext, NioUserLongConnection> item = iterator.next();
						NioUserLongConnection connection = item.getValue();
						ChannelHandlerContext ctx = item.getKey();
						if((System.currentTimeMillis() - connection.getActiveTime() >= time_out)
								|| !ctx.channel().isActive()){
							ctx.close();
							iterator.remove();
							SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							logger.info("====== remove "+ entry.getKey() + " set ActiveTime " + 
									sdf.format(connection.getActiveTime()) + " NowTime " + sdf.format(System.currentTimeMillis())
									+ " Active " + ctx.channel().isActive());
						}
					}
					if(connections.size() == 0) {
						it.remove();
						logger.info("====== remove "+ entry.getKey());
					}
				}
			}
		}
	}
}
