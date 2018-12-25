package com.quakoo.baseFramework.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Created by 136249 on 2015/4/13.
 */
public class FutureInterrupt {
    static Logger logger= LoggerFactory.getLogger(FutureInterrupt.class);

    static ConcurrentHashMap<Future,Long> map=new ConcurrentHashMap<>(10000000);

    public static void add(Future future,long time){
        try {
            if(!future.isCancelled()) {
                map.put(future,time);
            }
        }catch (Exception e){
            logger.error("",e);
        }
    }

    public static void remove(Future future){
        map.remove(future);
    }

    static {
        startInterruptFuture();
    }

    public static void startInterruptFuture(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (; ; ) {
                    long start = 0;
                    long processed = 0;
                    try {
                        start = map.size();
                        Iterator<Map.Entry<Future,Long>> iterator = map.entrySet().iterator();
                        while (iterator.hasNext()) {
                            Map.Entry<Future,Long> entry = iterator.next();
                            Future future=entry.getKey();
                            Long ltime=entry.getValue();
                            if(ltime==null){
                                continue;
                            }
                            long time = ltime;
                            if (time > System.currentTimeMillis()) {
                                continue;
                            }
                            if (!future.isCancelled()) {
                                future.cancel(true);
                                if (!future.isCancelled()) {
                                    logger.debug("cancelled a future fail :{}", future);
                                    continue;
                                } else {
                                    logger.debug("cancelled a future success :{}", future);
                                }
                            }
                            processed = processed + 1;
                            iterator.remove();
                        }
                    } catch (Exception e) {
                        logger.error("interruptFuture error", e);
                    } finally {
                        long end = map.size();
                        logger.debug("interruptFuture start:{} ,processed:{}, now:{}", start, processed, end);
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {

                        }
                    }
                }
            }
        }).start();

    }



    public static class FutureInfo{
        private long time;
        private Future future;

        public Future getFuture() {
            return future;
        }

        public void setFuture(Future future) {
            this.future = future;
        }

        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }

        public FutureInfo(long time, Future future) {
            this.time = time;
            this.future = future;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FutureInfo)) return false;

            FutureInfo that = (FutureInfo) o;

            if (future != null ? !future.equals(that.future) : that.future != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return future != null ? future.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "FutureInfo{" +
                    "time=" + time +
                    ", future=" + future +
                    '}';
        }
    }

}
