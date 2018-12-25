package com.quakoo.hbaseFramework;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import com.quakoo.baseFramework.redis.JedisX;


public class RedisAsyncInsertManager implements InitializingBean{
	
	Logger logger=LoggerFactory.getLogger(RedisAsyncInsertManager.class);
	
	
	private JedisX redis;

	private  int threadNum = 1;// 线程数目
	private  int mergeNum = 100000;// 最大合并数目
	private  int maxQueueSize=5000000;
	
    public int expireSecond = 60 * 60 * 24 * 30 * 2;//2月

    
    
	public JedisX getRedis() {
		return redis;
	}

	public void setRedis(JedisX redis) {
		this.redis = redis;
	}

	public int getThreadNum() {
		return threadNum;
	}

	public void setThreadNum(int threadNum) {
		this.threadNum = threadNum;
	}

	public int getMergeNum() {
		return mergeNum;
	}

	public void setMergeNum(int mergeNum) {
		this.mergeNum = mergeNum;
	}

	public int getMaxQueueSize() {
		return maxQueueSize;
	}

	public void setMaxQueueSize(int maxQueueSize) {
		this.maxQueueSize = maxQueueSize;
	}

	public int getExpireSecond() {
		return expireSecond;
	}

	public void setExpireSecond(int expireSecond) {
		this.expireSecond = expireSecond;
	}

	private  LinkedBlockingQueue<AsyncInsertValue> queueu = new  LinkedBlockingQueue<AsyncInsertValue>();

	public  void add(AsyncInsertValue asyncInsertValue) {
		if(queueu.size()>maxQueueSize){
			logger.error("queueu size over max :"+maxQueueSize);
			return;
		}
		queueu.add(asyncInsertValue);
	}

	public  void add(String key,Object value) {
		AsyncInsertValue asyncInsertValue=new AsyncInsertValue();
		asyncInsertValue.setKey(key);
		asyncInsertValue.setValue(value);
		asyncInsertValue.setExpireSecond(this.expireSecond);
		add(asyncInsertValue);
	}

	public void add(String key,Object value,int expireSecond){
		AsyncInsertValue asyncInsertValue=new AsyncInsertValue();
		asyncInsertValue.setKey(key);
		asyncInsertValue.setValue(value);
		asyncInsertValue.setExpireSecond(expireSecond);
		add(asyncInsertValue);
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		for(int i=0;i<threadNum;i++){
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					for (;;) {

						Map<Integer,Map<String,Object>> bs=new HashMap<Integer,Map<String,Object>>();
						try {
							for (int i = 0; i < mergeNum; i++) {
								AsyncInsertValue asyncInsertValue = queueu.poll();
								if (asyncInsertValue == null) {
									break;
								}
								int expireSecond=asyncInsertValue.getExpireSecond();
								Map<String,Object> map=bs.get(expireSecond);
								if(map==null){
									map=new HashMap<String, Object>();
									bs.put(expireSecond,map);
								}
								map.put(asyncInsertValue.getKey(),asyncInsertValue.getValue());
							}
							if (bs.size() > 0) {
								for(Map.Entry<Integer,Map<String,Object>> entry:bs.entrySet()) {
									if(entry.getValue()!=null&&entry.getValue().size()>0) {
										redis.multiSetObject(entry.getValue(), entry.getKey());
									}
								}
							} else {
								Thread.sleep(100);
							}

						} catch (Throwable e) {
							logger.error("redisAsyncInsert",e);
							try {
								Thread.sleep(100);
							} catch (InterruptedException e1) {
								logger.error("AsyncInsert thread sleep Error",e1.getMessage());
							}
						}
					}
					
				}
			}).start();
		}
		
	}

	

}