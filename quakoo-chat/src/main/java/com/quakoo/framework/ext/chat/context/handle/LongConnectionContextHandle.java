package com.quakoo.framework.ext.chat.context.handle;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.servlet.AsyncContext;

import com.quakoo.framework.ext.chat.model.UserStream;
import com.quakoo.framework.ext.chat.model.back.ConnectBack;
import com.quakoo.framework.ext.chat.model.back.StreamBack;
import com.quakoo.framework.ext.chat.model.param.UserLongConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.framework.ext.chat.service.ConnectService;
import com.quakoo.framework.ext.chat.service.UserStreamService;

public class LongConnectionContextHandle extends BaseContextHandle {

    Logger logger = LoggerFactory.getLogger(LongConnectionContextHandle.class);
	
	@Resource
	private UserStreamService userStreamService;
	
	@Resource
	private ConnectService connectService;
	
	private static final int num = Runtime.getRuntime().availableProcessors() * 2;
	
	public static final long time_out = 1000 * 60 * 2;
	
	public static volatile List<Map<Long, Set<UserLongConnection>>> connection_context  = Lists.newArrayList();
	
	private void init_context(){
		if(connection_context.size() == 0){
			for(int i = 0; i < num ; i++){
				Map<Long, Set<UserLongConnection>> pool = Maps.newConcurrentMap();
				connection_context.add(pool);
			}
		}
	}
	
	public static Map<Long, Set<UserLongConnection>> get_connection_pool(long uid){
		long index = uid % num;
		return connection_context.get((int)index);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.init_context();
		for(int i = 0 ; i < num ; i++){
			Map<Long, Set<UserLongConnection>> pool = connection_context.get(i);
			Processer processer = new Processer(pool);
	    	Thread handleThread = new Thread(processer);
	    	handleThread.start();
	    	Cleaner cleaner = new Cleaner(pool);
	    	Thread cleanThread = new Thread(cleaner);
	    	cleanThread.start();

		}
	}
	
	class Processer implements Runnable {
		
		private Map<Long, Set<UserLongConnection>> pool;
		
		public Processer(Map<Long, Set<UserLongConnection>> pool) {
			this.pool = pool;
		}
		
		private void send(UserLongConnection connection, String content) {
			if (connection.getSended())
				return;
			if (System.currentTimeMillis() - connection.getStartTime() >= time_out)
				return;
			AsyncContext asyncContext = connection.getAsyncContext();
			try {
				asyncContext.getResponse().setCharacterEncoding(
						"text/html;charset=UTF-8");
				asyncContext.getResponse().getOutputStream()
						.write(content.getBytes());
				asyncContext.getResponse().getOutputStream().flush();
				asyncContext.complete();
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			} finally {
				connection.getAndSetSended(true);
			}
		}
		
		private Double get_min_index (Set<UserLongConnection> set){
			Double res = null;
			if(null != set){
				for(UserLongConnection one : set){
					if((System.currentTimeMillis() - one.getStartTime() 
							>= time_out) || one.getSended()) {
						continue;
					}
					if (null == res || (one.getLastMsgSort() < res.doubleValue()))
						res = one.getLastMsgSort();
				}
			}
			return res;
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
		public void run() {
			while(true){
				Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
				try {
					if(pool.size() == 0){
						continue;
					}
					Map<Long, Set<UserLongConnection>> handle_map = Maps.newHashMap();
					Map<Long, Double> user_index_map = Maps.newHashMap();
					for(Entry<Long, Set<UserLongConnection>> entry : pool.entrySet()){
						long uid = entry.getKey();
						Double minIndex = get_min_index(entry.getValue());
						if(null != minIndex){
							handle_map.put(uid, entry.getValue());
							user_index_map.put(uid, minIndex);
						}
					}
					if(user_index_map.size() == 0) {
						continue;
					}
					Map<Long, List<UserStream>> streamMap = userStreamService.newStream(user_index_map);
					for(Entry<Long, Set<UserLongConnection>> entry : handle_map.entrySet()) {
						long uid = entry.getKey();
						List<UserStream> streams = streamMap.get(uid);
						if(null != streams) {
							for(UserLongConnection connect :entry.getValue()) {
								List<UserStream> sendList = filter(connect.getLastMsgSort(), streams);
								List<StreamBack> sendStreams = userStreamService.transformBack(sendList);
								ConnectBack connectBack = connectService.transformBack(sendStreams, null);
								if(connectBack.isSend()) {
									String json = JsonUtils.toJson(connectBack);
									send(connect, json);
								}
							}
						}
					}
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
	}
	
    class Cleaner implements Runnable{
		
        private Map<Long, Set<UserLongConnection>> pool;
		
		public Cleaner(Map<Long, Set<UserLongConnection>> pool){
			super();
			this.pool = pool;
		}

		@Override
		public void run() {
			while(true){
				Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
				long current_time = System.currentTimeMillis();
				for(Iterator<Entry<Long, Set<UserLongConnection>>> it =
						pool.entrySet().iterator(); it.hasNext();){
					Entry<Long, Set<UserLongConnection>> entry = it.next();
					Set<UserLongConnection> connections = entry.getValue();
					for(Iterator<UserLongConnection> iterator = connections.iterator();
							iterator.hasNext();){
						UserLongConnection connection = iterator.next();
						if((current_time - connection.getStartTime() >= time_out)
								|| connection.getSended()) {
							try {
								connection.getAsyncContext().complete();
							} catch (Exception e) {
							}
	                        iterator.remove();	
	                        logger.info("id : "+entry.getKey() + 
	                        		" remove one AsyncContext! have "+ connections.size());
						}
					}
					if(connections.size() == 0){
						it.remove();
						logger.info("id : " + entry.getKey() + " romove loog connection! pool Size : " + pool.size());
					}
				}
			}
		}
	}

}
