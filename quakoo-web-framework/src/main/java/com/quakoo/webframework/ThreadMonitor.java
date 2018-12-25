package com.quakoo.webframework;

import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.BlockingQueue;

/**
 * @author liyongbiao
 */
public class ThreadMonitor {
    static Logger logger = LoggerFactory.getLogger("monitor");

    private static volatile boolean busy = false;
    private static volatile String serverInfo = "";

    public static boolean isBusy() {
        return busy;
    }

    public static String getServerInfo() {
        return serverInfo;
    }

    private static void setBusy(boolean busy) {
        ThreadMonitor.busy = busy;
    }

    private static void setServerInfo(String serverInfo) {
        ThreadMonitor.serverInfo = serverInfo;
    }

    public static void init(final org.eclipse.jetty.server.Server jettyServer,
                            final double minWorkNumFactor, final double maxQueueSizeFactor,final int checkTime)
            throws Exception {

       if(checkTime > 0){
    	   new Thread(new Runnable() {

               @SuppressWarnings("unchecked")
               public void run() {
                   for (; ; ) {
                       try {
                           Thread.sleep(checkTime);
                       } catch (Exception e) {
                       }
                       try {
                           if (jettyServer == null
                                   || jettyServer.getThreadPool() == null)
                               continue;
                           final QueuedThreadPool pool = (QueuedThreadPool) jettyServer
                                   .getThreadPool();
                           int threads = pool.getThreads();
                           int idle = pool.getIdleThreads();
                           int maxQueued = pool.getMaxQueued();
                           int maxThreads = pool.getMaxThreads();
                           Method method = pool.getClass().getDeclaredMethod("getQueue");
                           method.setAccessible(true);
                           BlockingQueue<Runnable> blockingQueue = (BlockingQueue<Runnable>) method
                                   .invoke(pool);
                           if (blockingQueue == null)
                               continue;
                           int blockingQueueSize = blockingQueue.size();
                           int work = threads - idle;
                           if (minWorkNumFactor > 0) {
                               setBusy(work < (maxThreads * minWorkNumFactor));
                           } else if (maxQueueSizeFactor > 0) {
                               setBusy(blockingQueueSize > (maxThreads * maxQueueSizeFactor));
                           }
                           String serverInfo = "created:" + threads + ",work:" + work + ",queueSize:" + blockingQueueSize + ",idle:" + idle;
                           setServerInfo(serverInfo);
                           logger.info(
                                   "monitor jetty threads created:{},work:{},idle:{},queueSize:{},maxQueueSize:{},maxThreads:{}.",
                                   threads, work, idle, blockingQueueSize,
                                   maxQueued, maxThreads);
                       } catch (Exception e) {
                           e.printStackTrace();
                       }
                   }
               }
           }).start();
       }
    }

}
