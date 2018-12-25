package com.quakoo.baseFramework.lock;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 分布式互斥锁
 * Created by 136249 on 2015/2/10.
 */
public class ZkLock {
    Logger logger = LoggerFactory.getLogger(ZkLock.class);

    InterProcessMutex interProcessMutex;
    volatile CuratorFramework client;
    public static Map<String, CuratorFramework> clients = new HashMap<String, CuratorFramework>();

	public static int checkTime=1000*10;//10秒
    
    public final static String defaultLockPath="/defaultZkLockPath";

    private String myPath;
    /**
     * @param zkAddress   zookeeper地址
     * @param projectName 项目名称
     * @param lockPath    需要锁住的名字
     * @param sessionTimeoutMs    session时间毫秒（设置要比程序运行的时间长才行，否则网络异常的时候，zk超过该时间后，会让其他程序获得该锁）
     */
    public ZkLock(String zkAddress, String projectName, String lockPath, int sessionTimeoutMs) {
	    if(!lockPath.startsWith("/")){
	    		lockPath="/"+lockPath;
	    }
        lockPath= defaultLockPath+lockPath;
        this.myPath=lockPath;
        String key = zkAddress + projectName+sessionTimeoutMs;
        client = clients.get(key);
        if (client == null) {
            synchronized (clients) {
                if (client == null) {
                    CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder();
                    client = builder.connectString(zkAddress)
                            .sessionTimeoutMs(sessionTimeoutMs)
                            .connectionTimeoutMs(10000)
                            .canBeReadOnly(false)
                            .retryPolicy(new ExponentialBackoffRetry(10, 20))
                            .namespace(projectName)
                            .defaultData(null)
                            .build();
                    client.start();
                    CleanLockPath.addAllOldPath(client);
                    clients.put(key, client);
                }
            }
        }
        interProcessMutex = new InterProcessMutex(client, lockPath);
    }

    public void lock(long time, TimeUnit timeUnit) throws Exception {
    	long start=System.currentTimeMillis();
        boolean success=interProcessMutex.acquire(time, timeUnit);
        if(!success){
            throw new ZKLockTimeOutException();
        }
        logger.info("lock path:{},cost time:{}",myPath,(System.currentTimeMillis()-start));
    }

    /**
     * TimeUnit.MILLISECONDS
     * @param time 毫秒
     * @throws Exception
     */
	public void lock(long time) throws Exception {
    	lock(time, TimeUnit.MILLISECONDS);
    }
	
	public boolean tryLock(long time) throws Exception {
		long start = System.currentTimeMillis();
		boolean result = interProcessMutex.acquire(time, TimeUnit.MILLISECONDS);
		logger.info("tryLock {} path:{},cost time:{}", result, myPath, (System.currentTimeMillis() - start));
		return result;
	}

    public void release() {
        try {
            interProcessMutex.release();
            //delete if no Children
            client.delete().forPath(myPath);
        } catch (Exception e) {
            logger.warn("error in release lock", e);
        }
    }
    
    public static ZkLock getAndLock(String zkAddress,String projectName,String key,boolean runOnzkError,int sessionTimeOut,int lockTimeOut) throws Exception{
    	ZkLock zkLock=null;
    	try{
    		zkLock=new ZkLock(zkAddress,projectName,key,sessionTimeOut);
    	}catch(Exception e){
    		if(runOnzkError){
    			return zkLock;
    		}else{
    			throw e;
    		}
    	}
    	zkLock.lock(lockTimeOut);
    	return zkLock;
    }
    
    public static ZkLock getUnLocked(String zkAddress,String projectName,String key,boolean runOnzkError,int sessionTimeOut) throws Exception{
    	ZkLock zkLock=null;
    	try{
    		zkLock=new ZkLock(zkAddress,projectName,key,sessionTimeOut);
    	}catch(Exception e){
    		if(runOnzkError){
    			return zkLock;
    		}else{
    			throw e;
    		}
    	}
    	return zkLock;
    }
    
    
    
    public static void main(String[] few) throws Exception{
    	for(int i=100;i<101;i++){
	    	ZkLock lock=new ZkLock("172.28.6.135:2181,172.28.6.136:2181,172.28.6.137:2181", "test", "testLockPath"+i, 30000);
	    	lock.lock(1000);
	    	Thread.sleep(1000);
	    	lock.release();
    	}
    }

}
