package com.quakoo.framework.ext.chat.context.handle.distributed;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.ListUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.framework.ext.chat.context.handle.BaseContextHandle;
import com.quakoo.framework.ext.chat.distributed.DistributedConfig;

/**
 * 分布式调度上下文(调度多个服务器)
 * class_name: DistributedSchedulerContextHandle
 * package: com.quakoo.framework.ext.chat.context.handle.distributed
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 16:29
 **/
public class DistributedSchedulerContextHandle extends BaseContextHandle implements DisposableBean  {
	
	Logger logger = LoggerFactory.getLogger(DistributedSchedulerContextHandle.class);
	
	private static String chatHelpPath = "/chat_help";
	
	private String serialNumber; //本服务器序列号
	
	private CuratorFramework client;
	
	private PathChildrenCache cached;
	
	class Processer implements Runnable {
		@Override
		public void run() {
			while(true) {
				Uninterruptibles.sleepUninterruptibly(60, TimeUnit.SECONDS);
				try {
					List<String> serialNumbers = client.getChildren().forPath(chatHelpPath);
					serialNumbers = sortSerialNumbers(serialNumbers);
					handle(serialNumbers);
				} catch (Exception e) {
				}
			}
		}
	}

	private void initClient() throws Exception {
		CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory
				.builder();
		CuratorFramework client = builder
				.connectString(chatInfo.distributedZkAddress)
				.sessionTimeoutMs(20000)
				.connectionTimeoutMs(5000)
				.canBeReadOnly(false)
				.retryPolicy(
						new ExponentialBackoffRetry(1000, Integer.MAX_VALUE))
				.namespace(chatInfo.projectName).defaultData(null).build();
		client.start();
		Stat stat=client.checkExists().forPath(chatHelpPath);
		if(null == stat){
			client.create().creatingParentsIfNeeded().
					withMode(CreateMode.PERSISTENT).forPath(chatHelpPath);
		}
		String path = chatHelpPath + "/" + serialNumber;
		client.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(path);
	    cached = new PathChildrenCache(client, chatHelpPath, true);
		cached.getListenable().addListener(new PathChildrenCacheListener() {
			@Override
			public void childEvent(CuratorFramework client,
					PathChildrenCacheEvent event) throws Exception {
				PathChildrenCacheEvent.Type eventType = event.getType();
				switch (eventType) {
				case CONNECTION_RECONNECTED: {
					cached.rebuild();
					break;
				}
				case CHILD_ADDED: {
					List<String> serialNumbers = getSerialNumbers(cached.getCurrentData());
					handle(serialNumbers);
					break;
				}
				case CHILD_UPDATED: {
					List<String> serialNumbers = getSerialNumbers(cached.getCurrentData());
					handle(serialNumbers);
					break;
				}
				case CHILD_REMOVED: {
					List<String> serialNumbers = getSerialNumbers(cached.getCurrentData());
					handle(serialNumbers);
					break;
				}
				default:
					break;
				}
			}
		});
		cached.start(StartMode.BUILD_INITIAL_CACHE);
		List<String> serialNumbers = getSerialNumbers(cached.getCurrentData());
		handle(serialNumbers);
		this.client = client;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		serialNumber = UUID.randomUUID().toString();
		logger.error("====  serialNumber : " + serialNumber);
		this.initClient();
		Thread processer = new Thread(new Processer());
		processer.start();
	}
	
	private List<String> getSerialNumbers(List<ChildData> childDatas) {
		List<String> serialNumbers = Lists.newArrayList();
		if (childDatas != null) {  
		    for (ChildData data : childDatas) {  
		    	String path = data.getPath();
		    	serialNumbers.add(path.substring(path.lastIndexOf("/")+1));
		    }  
		} 
		return sortSerialNumbers(serialNumbers);
	}
	
	private List<String> sortSerialNumbers(List<String> serialNumbers) {
		int length = serialNumber.length();
	    Map<Long, String> map = Maps.newTreeMap();
	    for(String serialNumber : serialNumbers) {
	    	long sort = Long.parseLong(serialNumber.substring(length));
	    	map.put(sort, serialNumber);
	    }
	    return Lists.newArrayList(map.values());
	}
	
	private void handle(List<String> serialNumbers) {
		if(serialNumbers.size() > 0) {
			int serverNum = serialNumbers.size();
			int serverIndex = -1;
			for(int i = 0; i < serverNum; i++) {
				if(serialNumbers.get(i).startsWith(serialNumber)){
					serverIndex = i;
					break;
				}
			}
			if(serverIndex >= 0) {
//				List<String> canRunManyChatTable = getHandleTableNames(chatInfo.many_chat_queue_table_names,
//						serverNum, serverIndex);
                List<String> canRunManyQueue = getHandleTableNames(chatInfo.many_chat_queue_names, serverNum, serverIndex);
//				boolean sign = ListUtils.isEqualList(DistributedConfig.canRunManyChatTable, canRunManyChatTable);
                boolean sign = ListUtils.isEqualList(DistributedConfig.canRunManyQueue, canRunManyQueue);
				if(!sign)
//					DistributedConfig.canRunManyChatTable = canRunManyChatTable;
                    DistributedConfig.canRunManyQueue = canRunManyQueue;
				
//				List<String> canRunSingleChatTable = getHandleTableNames(chatInfo.single_chat_queue_table_names,
//						serverNum, serverIndex);
                List<String> canRunSingleQueue = getHandleTableNames(chatInfo.single_chat_queue_names, serverNum, serverIndex);
//				sign = ListUtils.isEqualList(DistributedConfig.canRunSingleChatTable, canRunSingleChatTable);
                sign = ListUtils.isEqualList(DistributedConfig.canRunSingleQueue, canRunSingleQueue);
				if(!sign)
//					DistributedConfig.canRunSingleChatTable = canRunSingleChatTable;
                    DistributedConfig.canRunSingleQueue = canRunSingleQueue;


                List<String> canRunUserStreamQueue = getHandleTableNames(chatInfo.user_stream_queue_names, serverNum, serverIndex);
                sign = ListUtils.isEqualList(DistributedConfig.canRunUserStreamQueue, canRunUserStreamQueue);
                if(!sign)
                    DistributedConfig.canRunUserStreamQueue = canRunUserStreamQueue;

                List<String> canRunUserInfoQueue = getHandleTableNames(chatInfo.user_info_queue_names, serverNum, serverIndex);
                sign = ListUtils.isEqualList(DistributedConfig.canRunUserInfoQueue, canRunUserInfoQueue);
                if(!sign)
                    DistributedConfig.canRunUserInfoQueue = canRunUserInfoQueue;

				if(serverIndex == 0) {
					DistributedConfig.canRunNoticeAll = true;
				} else {
					DistributedConfig.canRunNoticeAll = false;
				}
				
				if(serverIndex == (serverNum - 1)) {
					DistributedConfig.canRunNoticeRange = true;
				} else {
					DistributedConfig.canRunNoticeRange = false;
				}
				
//				if(serverNum >= 3) {
//					if(serverIndex == 1) DistributedConfig.canRunPush = true;
//					else DistributedConfig.canRunPush = false;
//				} else {
//					if(serverIndex == 0) DistributedConfig.canRunPush = true;
//					else DistributedConfig.canRunPush = false;
//				}

                if (serverNum >= 3) {
                    if (serverIndex == 1) DistributedConfig.canRunClean = true;
                    else DistributedConfig.canRunClean = false;
                } else {
                    if (serverIndex == 0) DistributedConfig.canRunClean = true;
                    else DistributedConfig.canRunClean = false;
                }
				
				if(serverNum >= 4) {
					if(serverIndex == 2) DistributedConfig.canRunWillPush = true;
					else DistributedConfig.canRunWillPush = false;
				} else if(serverNum == 3) {
					if(serverIndex == 1) DistributedConfig.canRunWillPush = true;
					else DistributedConfig.canRunWillPush = false;
				} else {
					if(serverIndex == (serverNum - 1)) DistributedConfig.canRunWillPush = true;
					else DistributedConfig.canRunWillPush = false;
				}
			}
		}
	}
	
	private List<String> getHandleTableNames(List<String> allTableNames, int serverNum, int serverIndex) {
		List<List<String>> partitionTableNames = partition(allTableNames, serverNum);
		List<String> handleTableNames = partitionTableNames.get(serverIndex);
		logger.info("====  handleNames : " + handleTableNames.toString());
		return handleTableNames;
	}
	
	private <T> List<List<T>> partition(List<T> source, int listNum) {
		List<List<T>> res = Lists.newArrayList();
		for(int i = 0; i < listNum; i++) {
			List<T> one = Lists.newArrayList();
			res.add(one);
		}
		for(int i = 0; i < source.size(); i++) {
			int times =  i/listNum;
			int listIndex = i - times * listNum;
			res.get(listIndex).add(source.get(i));
		}
		return res;
	}

	@Override
	public void destroy() throws Exception {
		if(null != this.cached) this.cached.close();
		if(null != this.client) this.client.close();
	}
	
}
