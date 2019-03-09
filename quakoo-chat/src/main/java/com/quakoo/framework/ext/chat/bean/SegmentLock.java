//package com.quakoo.framework.ext.chat.bean;
//
//import java.util.HashMap;
//import java.util.concurrent.locks.ReentrantLock;
//
//public class SegmentLock {
//
//    private Integer segments = 4;//默认分段数量
//    private final HashMap<Integer, ReentrantLock> lockMap = new HashMap<>();
//
//    public SegmentLock() {
//        init(null, false);
//    }
//
//    public SegmentLock(Integer counts, boolean fair) {
//        init(counts, fair);
//    }
//
//    private void init(Integer counts, boolean fair) {
//        if (counts != null) {
//            segments = counts;
//        }
//        for (int i = 0; i < segments; i++) {
//            lockMap.put(i, new ReentrantLock(fair));
//        }
//    }
//
//    public void lock(Object key) {
//        ReentrantLock lock = lockMap.get((key.hashCode()>>>1) % segments);
//        lock.lock();
//    }
//
//    public void unlock(Object key) {
//        ReentrantLock lock = lockMap.get((key.hashCode()>>>1) % segments);
//        lock.unlock();
//    }
//
//
//
//}
