package com.quakoo.baseFramework.thread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class NamedThreadFactory implements ThreadFactory {
    private static final AtomicInteger _instanceNumber = new AtomicInteger();
    private final AtomicInteger _threadNumber = new AtomicInteger();
    private final String _namePrefix;

    public NamedThreadFactory(String name) {
        _namePrefix = name + "-" + _instanceNumber.incrementAndGet();
    }

    public Thread newThread(Runnable runnable) {
        Thread newThread;
        String name = _namePrefix + "-" + _threadNumber.incrementAndGet();
        newThread = new Thread(runnable, name);
        return newThread;
    }
}
