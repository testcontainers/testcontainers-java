package org.testcontainers.containers.wait.internal;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

class ThreadFactories {
    static ThreadFactory prefixedThreadFactory(String prefix) {
        return new ThreadFactory() {
            private final AtomicLong seq = new AtomicLong(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, prefix + "-" + seq.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        };
    }
}
