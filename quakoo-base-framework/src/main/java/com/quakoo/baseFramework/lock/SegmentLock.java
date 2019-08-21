package com.quakoo.baseFramework.lock;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class SegmentLock <T> {

    private Integer segments = 16;//默认分段数量
    private final HashMap<Integer, ReentrantLock> lockMap = new HashMap<>();

    public SegmentLock() {
        init(null, false);
    }

    public SegmentLock(Integer counts, boolean fair) {
        init(counts, fair);
    }

    private void init(Integer counts, boolean fair) {
        if (counts != null) {
            segments = counts;
        }
        for (int i = 0; i < segments; i++) {
            lockMap.put(i, new ReentrantLock(fair));
        }
    }

    public void lock(T key) {
        ReentrantLock lock = lockMap.get((key.hashCode()>>>1) % segments);
        lock.lock();
    }

    public void unlock(T key) {
        ReentrantLock lock = lockMap.get((key.hashCode()>>>1) % segments);
        lock.unlock();
    }

}