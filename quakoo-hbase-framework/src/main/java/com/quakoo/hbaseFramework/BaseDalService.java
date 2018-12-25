package com.quakoo.hbaseFramework;

import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.baseFramework.thread.NamedThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by 136249 on 2015/3/17.
 */
public class BaseDalService {



    public static final ExecutorService redisExecutor = new ThreadPoolExecutor(32, 512, 20, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>(), new NamedThreadFactory("baseDalService_redis_thread"),
            new ThreadPoolExecutor.AbortPolicy());

    public static final ExecutorService hbaseExecutor = new ThreadPoolExecutor(32, 512, 20, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>(), new NamedThreadFactory("baseDalService_hbase_thread"),
            new ThreadPoolExecutor.AbortPolicy());

    public RedisAsyncInsertManager redisAsyncInsertManager;
    public JedisX redis;
    public int redisTimeOut = 100;
    public HbaseClient hbaseClient;
    public StepMaxValueIncrementer stoMaxValueIncrementer;
    public int threadAwaitTime = 2000;
    public boolean openRedis;

    public JedisX getRedis() {
        return redis;
    }

    public void setRedis(JedisX redis) {
        this.redis = redis;
    }

    public StepMaxValueIncrementer getStoMaxValueIncrementer() {
        return stoMaxValueIncrementer;
    }

    public void setStoMaxValueIncrementer(StepMaxValueIncrementer stoMaxValueIncrementer) {
        this.stoMaxValueIncrementer = stoMaxValueIncrementer;
    }

    public boolean isOpenRedis() {
        return openRedis;
    }

    public void setOpenRedis(boolean openRedis) {
        this.openRedis = openRedis;
    }

    public HbaseClient getHbaseClient() {
        return hbaseClient;
    }

    public void setHbaseClient(HbaseClient hbaseClient) {
        this.hbaseClient = hbaseClient;
    }

  
    public int getRedisTimeOut() {
        return redisTimeOut;
    }

    public void setRedisTimeOut(int redisTimeOut) {
        this.redisTimeOut = redisTimeOut;
    }

    

    public int getThreadAwaitTime() {
        return threadAwaitTime;
    }

    public void setThreadAwaitTime(int threadAwaitTime) {
        this.threadAwaitTime = threadAwaitTime;
    }

	public RedisAsyncInsertManager getRedisAsyncInsertManager() {
		return redisAsyncInsertManager;
	}

	public void setRedisAsyncInsertManager(
			RedisAsyncInsertManager redisAsyncInsertManager) {
		this.redisAsyncInsertManager = redisAsyncInsertManager;
	}


    





}
