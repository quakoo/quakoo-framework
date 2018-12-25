package com.quakoo.baseFramework.redis.util;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 
 * @author LiYongbiao
 *
 */
public class ThreadPool {
    private ExecutorService executorService = Executors.newFixedThreadPool(50);

    private static volatile ThreadPool _instance = null;

    public static ThreadPool getInstance() {
        if (_instance == null) {
            synchronized (ThreadPool.class) {
                if (_instance == null) {
                    _instance = new ThreadPool();
                }
            }
        }
        return _instance;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    private ThreadPool() {

    }
}
