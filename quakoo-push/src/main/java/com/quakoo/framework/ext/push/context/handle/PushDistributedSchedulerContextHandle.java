package com.quakoo.framework.ext.push.context.handle;

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
import org.springframework.beans.factory.DisposableBean;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.framework.ext.push.distributed.DistributedConfig;


public class PushDistributedSchedulerContextHandle extends PushBaseContextHandle implements DisposableBean {
	
    private String pushHelpPath;
	
	private String serialNumber;
	
	private CuratorFramework client;
	
	private PathChildrenCache cached;

	@Override
	public void afterPropertiesSet() throws Exception {
        pushHelpPath = "/push_help_" + pushInfo.projectName;
		serialNumber = UUID.randomUUID().toString();
		this.initClient();
		Thread processer = new Thread(new Processer());
		processer.start();
	}

	@Override
	public void destroy() throws Exception {
		if(null != this.cached) this.cached.close();
		if(null != this.client) this.client.close();
	}
	
	class Processer implements Runnable {
		@Override
		public void run() {
			while(true) {
				Uninterruptibles.sleepUninterruptibly(120, TimeUnit.SECONDS);
				try {
					List<String> serialNumbers = client.getChildren().forPath(pushHelpPath);
					int serverNum = serialNumbers.size();
					DistributedConfig.serverNum = serverNum;
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
				.connectString(pushInfo.distributedZkAddress)
				.sessionTimeoutMs(20000)
				.connectionTimeoutMs(5000)
				.canBeReadOnly(false)
				.retryPolicy(
						new ExponentialBackoffRetry(1000, Integer.MAX_VALUE))
				.namespace(pushInfo.projectName).defaultData(null).build();
		client.start();
		Stat stat=client.checkExists().forPath(pushHelpPath);
		if(null == stat){
			client.create().creatingParentsIfNeeded().
					withMode(CreateMode.PERSISTENT).forPath(pushHelpPath);
		}
		String path = pushHelpPath + "/" + serialNumber;
		client.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(path);
	    cached = new PathChildrenCache(client, pushHelpPath, true);
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
	
	private void handle(List<String> serialNumbers) {
		if(serialNumbers.size() > 0) {
			int serverNum = serialNumbers.size();
			DistributedConfig.serverNum = serverNum;
			int serverIndex = -1;
			for(int i = 0; i < serverNum; i++) {
				if(serialNumbers.get(i).startsWith(serialNumber)){
					serverIndex = i;
					break;
				}
			}
			if(serverIndex >= 0) {
				List<String> canRunHandleQueueTable = getHandleTableNames(pushInfo.push_handle_queue_table_names,
						serverNum, serverIndex);
				boolean sign = ListUtils.isEqualList(DistributedConfig.canRunHandleQueueTable,
						canRunHandleQueueTable);
				if(!sign)
					DistributedConfig.canRunHandleQueueTable = canRunHandleQueueTable;
				
				List<String> canRunUserQueueTable = getHandleTableNames(pushInfo.push_user_queue_table_names, 
						serverNum, serverIndex);
				sign = ListUtils.isEqualList(DistributedConfig.canRunUserQueueTable, canRunUserQueueTable);
				if(!sign)
					DistributedConfig.canRunUserQueueTable = canRunUserQueueTable;
			}
		}
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
	
	private List<String> getHandleTableNames(List<String> allTableNames, int serverNum, int serverIndex) {
		List<List<String>> partitionTableNames = partition(allTableNames, serverNum);
		List<String> handleTableNames = partitionTableNames.get(serverIndex);
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

}
