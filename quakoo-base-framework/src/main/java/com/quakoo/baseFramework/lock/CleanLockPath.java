package com.quakoo.baseFramework.lock;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CleanLockPath {

	static Logger logger = LoggerFactory.getLogger(CleanLockPath.class);

	private static final int checkAllTime=600;//10分钟
	static List<CuratorFramework> clients=new CopyOnWriteArrayList<CuratorFramework>();
	
	static LinkedBlockingQueue<CleanLock> cleanQueue = new LinkedBlockingQueue<CleanLock>(1000000);

	public static void add(CuratorFramework client, String path, long time) {
		
		add( new CleanLock(client, path, time));

	}
	
	private  static void add(CleanLock cleanLock) {
		try{
			cleanQueue.add( cleanLock);
		}catch(Exception e){
			logger.error("",e);
		}

	}
	

	// 系统重启的时候有很多path没有删除掉，在这里进行删除
	public static void addAllOldPath(CuratorFramework client) {
		try {
			if(!clients.contains(client)){
				clients.add(client);
			}
			Stat stat = client.checkExists().forPath(ZkLock.defaultLockPath);
			if(null != stat){
				List<String> paths = client.getChildren().forPath(
						ZkLock.defaultLockPath);
				for (String subPAth : paths) {
					add(client, ZkLock.defaultLockPath + "/" + subPAth,
							(System.currentTimeMillis() + (ZkLock.checkTime)));
				}
				logger.debug("addAllOldPath addSize:{},allSize:{}",paths.size(),cleanQueue.size());
			}
		} catch (Exception e) {
			logger.error("addAllOldPath error", e);
		}
	}

	static {
		System.setProperty("jute.maxbuffer",Integer.toString(1024*1024*512));

		int startTime=new Random().nextInt(checkAllTime);
		 ScheduledExecutorService cleanCacheSchedule = Executors.newSingleThreadScheduledExecutor();
	        cleanCacheSchedule.scheduleAtFixedRate(new Runnable() {
	            @Override
	            public void run() {
	            	for(CuratorFramework client:clients){
	            		addAllOldPath(client);
	            	}
	            }
	        }, startTime, checkAllTime, TimeUnit.SECONDS);
		
		
		for(int i=0;i<10;i++){
			new Thread(new Runnable() {
	
				@Override
				public void run() {
					for (;;) {
						CleanLock cleanLock=null;
						try {
							cleanLock=cleanQueue.take();
							if (System.currentTimeMillis() > cleanLock.getTime()) {
								cleanLock.getClient().delete().forPath(cleanLock.getPath());
							}else{
								cleanQueue.add(cleanLock);
								Thread.sleep(50);
							}
						} catch (Exception e) {
							if(e instanceof NoNodeException){
								//do nothing
							}else  if(e instanceof KeeperException.NotEmptyException){
								cleanLock.setTime(System.currentTimeMillis()+ZkLock.checkTime);
								cleanQueue.add(cleanLock);
							}else{
								logger.error("clean error",e);
								try {
									Thread.sleep(50);
								} catch (InterruptedException e1) {
									e.printStackTrace();
								}
							}
						}
						
					}
				}
			}).start();
		}
	}

	public static class CleanLock {
		private CuratorFramework client;
		private String path;
		private long time;

		public CuratorFramework getClient() {
			return client;
		}

		public void setClient(CuratorFramework client) {
			this.client = client;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public long getTime() {
			return time;
		}

		public void setTime(long time) {
			this.time = time;
		}

		public CleanLock(CuratorFramework client, String path, long time) {
			super();
			this.client = client;
			this.path = path;
			this.time = time;
		}

	}

}
