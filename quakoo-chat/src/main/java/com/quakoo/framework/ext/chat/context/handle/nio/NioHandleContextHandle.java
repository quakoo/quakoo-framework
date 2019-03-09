package com.quakoo.framework.ext.chat.context.handle.nio;

import io.netty.channel.ChannelHandlerContext;

import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.framework.ext.chat.context.handle.BaseContextHandle;
import com.quakoo.framework.ext.chat.model.UserPrompt;
import com.quakoo.framework.ext.chat.model.UserStream;
import com.quakoo.framework.ext.chat.model.back.ConnectBack;
import com.quakoo.framework.ext.chat.model.back.PromptBack;
import com.quakoo.framework.ext.chat.model.back.StreamBack;
import com.quakoo.framework.ext.chat.model.param.nio.ConnectResponse;
import com.quakoo.framework.ext.chat.model.param.nio.NioPromptQueueItem;
import com.quakoo.framework.ext.chat.model.param.nio.NioUserLongConnection;
import com.quakoo.framework.ext.chat.nio.ChannelUtils;
import com.quakoo.framework.ext.chat.service.ConnectService;
import com.quakoo.framework.ext.chat.service.UserInfoService;
import com.quakoo.framework.ext.chat.service.UserPromptService;
import com.quakoo.framework.ext.chat.service.UserStreamService;

/**
 * socket处理基类上下文
 * class_name: NioHandleContextHandle
 * package: com.quakoo.framework.ext.chat.context.handle.nio
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 16:30
 **/
public abstract class NioHandleContextHandle extends BaseContextHandle {
	
	Logger logger = LoggerFactory.getLogger(NioHandleContextHandle.class);
	
	public static volatile LinkedBlockingQueue<NioPromptQueueItem> prompt_queue = 
			new LinkedBlockingQueue<NioPromptQueueItem>();
	
	public static volatile Map<ChannelHandlerContext, NioUserLongConnection> connection_context  = Maps.newConcurrentMap(); //socket连接字典

	public static final long time_out = 1000 * 60 * 2; //超时时间，自动清理
	
	private final int threadNum = Runtime.getRuntime().availableProcessors() * 2 + 1;
	
	private final int handleNum = 100;
	
	private ExecutorService executorService = Executors.newFixedThreadPool(threadNum);

	private CompletionService<Boolean> completionService = 
			new ExecutorCompletionService<Boolean>(executorService);
	
	@Resource
	private UserStreamService userStreamService;
	
	@Resource
	private ConnectService connectService;
	
	@Resource
	private UserPromptService userPromptService;
	
	@Resource
	private UserInfoService userInfoService;
	
	protected abstract void startConnectBootstrap();
	
	@Override
	public void afterPropertiesSet() throws Exception {
		startConnectBootstrap();
		Thread chatProcesser = new Thread(new ChatProcesser());
		chatProcesser.start();
		Thread cleaner = new Thread(new Cleaner());
		cleaner.start();
		for (int i = 0; i < threadNum; i++) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					while(true) {
						NioPromptQueueItem promptQueueItem = null;
						try {
							promptQueueItem = prompt_queue.take();
							handlePrompt(promptQueueItem);
						} catch (Exception e) {
							logger.error(e.getMessage(), e);
						}
					}
				}
			}).start();
		}
	}
	
	private void handlePrompt(NioPromptQueueItem promptQueueItem) {
		try {
			ChannelHandlerContext ctx = promptQueueItem.getCtx();
			long uid = promptQueueItem.getUid();
			double lastPromptIndex = promptQueueItem.getLastPromptIndex();
			List<UserPrompt> newPrompt = userPromptService.newPrompt(uid, lastPromptIndex);
			if(newPrompt.size() > 0) {
				double currentPromptIndex = newPrompt.get(0).getSort() + 0.001;
				userInfoService.updatePromptIndex(uid, currentPromptIndex);
			}
			List<PromptBack> prompts = userPromptService.transformBack(newPrompt);
			ConnectBack connectBack = connectService.transformBack(null, prompts);
			if(connectBack.isSend()) {
				ChannelUtils.write(ctx, new ConnectResponse(connectBack), false);
			} 
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	private void removeChannelHandlerContext(ChannelHandlerContext ctx) {
		Iterator<ChannelHandlerContext> iterator = connection_context.keySet().iterator();
		while (iterator.hasNext()) {  
			ChannelHandlerContext key = iterator.next();
			if(ctx.equals(key)) {
				iterator.remove();
				connection_context.remove(key);
			}
		}
	}
	
	private List<List<Entry<ChannelHandlerContext, NioUserLongConnection>>>
	        transformThreadHandleList(Map<ChannelHandlerContext, NioUserLongConnection> handleMap) {
		List<Entry<ChannelHandlerContext, NioUserLongConnection>> list = 
				Lists.newArrayList(handleMap.entrySet());
		int totalNum = list.size();
		int threadListNum = (totalNum%threadNum == 0) ? totalNum/threadNum : totalNum/threadNum + 1;
		List<List<Entry<ChannelHandlerContext, NioUserLongConnection>>> res = 
				Lists.partition(list, threadListNum);
		return res;
	}
	
	/**
     * IM子处理线程(拉模式，根据socket字典主动拉取用户的消息流)
	 * class_name: NioHandleContextHandle
	 * package: com.quakoo.framework.ext.chat.context.handle.nio
	 * creat_user: lihao
	 * creat_date: 2019/1/29
	 * creat_time: 16:35
	 **/
	class SubChatProcesser implements Callable<Boolean> {
		
		private List<Entry<ChannelHandlerContext, NioUserLongConnection>> handleList;
		
		public SubChatProcesser(List<Entry<ChannelHandlerContext, NioUserLongConnection>> handleList) {
			this.handleList = handleList;
		}
		
		private void send(ChannelHandlerContext ctx, ConnectResponse connectResponse, long activeTime) {
			if (!ctx.channel().isActive())
				return;
			if (System.currentTimeMillis() - activeTime >= time_out)
				return;
			ChannelUtils.write(ctx, connectResponse, false);
		}

		private List<UserStream> filter(double index, List<UserStream> list){
			List<UserStream> res = Lists.newArrayList();
			for(UserStream one : list){
				if(one.getSort() >= index){
					res.add(one);
				}
			}
			return res;
		}

		@Override
		public Boolean call() throws Exception {
			try {
				List<List<Entry<ChannelHandlerContext, NioUserLongConnection>>> partitionList = 
						Lists.partition(handleList, handleNum);
				for(Iterator<List<Entry<ChannelHandlerContext, NioUserLongConnection>>> it = 
						partitionList.iterator(); it.hasNext();) {
					List<Entry<ChannelHandlerContext, NioUserLongConnection>> list = it.next();
					Map<Long, Double> user_index_map = Maps.newHashMap();
					for(Entry<ChannelHandlerContext, NioUserLongConnection> one : list) {
						NioUserLongConnection nioUserLongConnection = one.getValue();
						long uid = nioUserLongConnection.getUid();
						double index = nioUserLongConnection.getLastMsgSort();
						Double preIndex = user_index_map.get(uid);
						if(null == preIndex) {
							user_index_map.put(uid, index);
						} else {
							if(preIndex.doubleValue() > index) {
								user_index_map.put(uid, index);
							}
						}
					}

					long startTime = System.currentTimeMillis();
					Map<Long, List<UserStream>> streamMap = userStreamService.newStream(user_index_map);
					List<ChannelHandlerContext> removeList = Lists.newArrayList();
					List<NioUserLongConnection> sendConns = Lists.newArrayList();
					for(Entry<ChannelHandlerContext, NioUserLongConnection> one : list) {
						ChannelHandlerContext ctx = one.getKey();
						NioUserLongConnection nioUserLongConnection = one.getValue();
						long uid = nioUserLongConnection.getUid();
						double index = nioUserLongConnection.getLastMsgSort();
						long activeTime = nioUserLongConnection.getActiveTime();
						List<UserStream> streams = streamMap.get(uid);
						if(null != streams) {
							List<UserStream> sendList = filter(index, streams);
							List<StreamBack> sendStreams = userStreamService.transformBack(sendList);
							ConnectBack connectBack = connectService.transformBack(sendStreams, null);
							if(connectBack.isSend()) {
								send(ctx, new ConnectResponse(connectBack), activeTime);
                                sendConns.add(nioUserLongConnection);
								removeList.add(ctx);
							}
						}
					}
					if(sendConns.size() > 0)
					    logger.info("==== sign monitoring send time : " + (System.currentTimeMillis() - startTime) +
                                " ,sendConns : " + sendConns.toString());
					for(ChannelHandlerContext ctx : removeList) {
						removeChannelHandlerContext(ctx);
					}
				}
				return true;
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
			return false;
		}
	}

	/**
     * IM处理线程
	 * class_name: NioHandleContextHandle
	 * package: com.quakoo.framework.ext.chat.context.handle.nio
	 * creat_user: lihao
	 * creat_date: 2019/1/29
	 * creat_time: 16:33
	 **/
	class ChatProcesser implements Runnable {
		@Override
		public void run() {
			while(true) {
				Uninterruptibles.sleepUninterruptibly(200, TimeUnit.MILLISECONDS);
				try {
					if(connection_context.size() == 0){
						continue;
					}
					Map<ChannelHandlerContext, NioUserLongConnection> handleMap = Maps.newHashMap();
					handleMap.putAll(connection_context);
					List<List<Entry<ChannelHandlerContext, NioUserLongConnection>>> threadHandleList = 
							transformThreadHandleList(handleMap);
					for(int i = 0; i < threadHandleList.size(); i++) {
						List<Entry<ChannelHandlerContext, NioUserLongConnection>> handleList = threadHandleList.get(i);
						completionService.submit(new SubChatProcesser(handleList));
					}
					for (int i = 0; i < threadHandleList.size(); i++) {
					    completionService.take().get();
					}
					handleMap.clear();
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
	}

	/**
     * 清理线程
	 * class_name: NioHandleContextHandle
	 * package: com.quakoo.framework.ext.chat.context.handle.nio
	 * creat_user: lihao
	 * creat_date: 2019/1/29
	 * creat_time: 16:32
	 **/
	class Cleaner implements Runnable {

	    private DecimalFormat decimalFormat = new DecimalFormat("###################.###");

		@Override
		public void run() {
            int i = 1;
			while(true) {
				Uninterruptibles.sleepUninterruptibly(1000, TimeUnit.MILLISECONDS);
				long currentTime = System.currentTimeMillis();
				Set<ChannelHandlerContext> removeSet = Sets.newHashSet();
				for(Entry<ChannelHandlerContext, NioUserLongConnection> entry : 
					connection_context.entrySet()) {
					ChannelHandlerContext ctx = entry.getKey();
					NioUserLongConnection nioUserLongConnection = entry.getValue();
					if(!ctx.channel().isActive()) {
						removeSet.add(ctx);
					} else {
						long activeTime = nioUserLongConnection.getActiveTime();
						if((currentTime - activeTime) > time_out)
							removeSet.add(ctx); 
					}
				}
				for(ChannelHandlerContext ctx : removeSet) {
					removeChannelHandlerContext(ctx);
					ctx.close();
				}
				if(i++ >= 60) {
                    for(Entry<ChannelHandlerContext, NioUserLongConnection> entry : connection_context.entrySet()) {
                        NioUserLongConnection info = entry.getValue();
                        logger.info("==== sign monitoring long connection info uid : " + info.getUid() + " ,index : " + decimalFormat.format(info.getLastMsgSort()));
                    }
                    i = 1;
                }

				logger.info("=== connection_context size : "+ connection_context.size());
			}
		}
	}
	
	public static void main(String[] args) {
        DecimalFormat decimalFormat = new DecimalFormat("###################.###");
	    double d = 1536549328863.002d;
        System.out.println(decimalFormat.format(d));
//		
//		int threadNum = 3;
//		Map<Integer, Integer> maps = Maps.newConcurrentMap();
//		for(int i = 1; i <= 1; i++) {
//			maps.put(i, i);
//		}
//		int threadListNum = maps.size()%threadNum == 0 ? maps.size()/threadNum : maps.size()/threadNum + 1;
//		List<Entry<Integer, Integer>> list = Lists.newArrayList(maps.entrySet());
//		List<List<Entry<Integer, Integer>>> parentList = Lists.partition(list, threadListNum);
//		System.out.println(parentList.size());
//		for(List<Entry<Integer, Integer>> subList : parentList) {
//		    System.out.println("============= " + subList.size());
//		    for(Entry<Integer, Integer> entry : subList) {
//		    	System.out.println(entry.toString());
//		    }
//		}
	}
	
}
