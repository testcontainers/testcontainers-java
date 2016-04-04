package org.testcontainers.utility;

import com.google.common.util.concurrent.Futures;
import lombok.SneakyThrows;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.rnorth.visibleassertions.VisibleAssertions.*;

public class LazyFutureTest {

    @Test
    public void testLazyness() throws Exception {
        AtomicInteger counter = new AtomicInteger();

        Future<Integer> lazyFuture = new LazyFuture<Integer>() {
            @Override
            protected Integer resolve() {
                return counter.incrementAndGet();
            }
        };

        assertEquals("No resolve() invocations before get()", 0, counter.get());
        assertEquals("get() call returns proper result", 1, lazyFuture.get());
        assertEquals("resolve() was called only once after single get() call", 1, counter.get());

        counter.incrementAndGet();
        assertEquals("result of resolve() must be cached", 1, lazyFuture.get());
    }

    @Test(timeout = 5_000)
    public void timeoutWorks() throws Exception {
        Future<Void> lazyFuture = new LazyFuture<Void>() {
            @Override
            @SneakyThrows(InterruptedException.class)
            protected Void resolve() {
                TimeUnit.MINUTES.sleep(1);
                return null;
            }
        };

        assertThrows("Should timeout", TimeoutException.class, () -> lazyFuture.get(10, TimeUnit.MILLISECONDS));
        pass("timeout works");
    }

    @Test(timeout = 5_000)
    public void testThreadSafety() throws Exception {
        final int numOfThreads = 3;
        CountDownLatch latch = new CountDownLatch(numOfThreads);
        AtomicInteger counter = new AtomicInteger();

        Future<Integer> lazyFuture = new LazyFuture<Integer>() {
            @Override
            @SneakyThrows(InterruptedException.class)
            protected Integer resolve() {
                latch.await();
                return counter.incrementAndGet();
            }
        };

        Future<List<Integer>> task = new ForkJoinPool(numOfThreads).submit(() -> {
            return IntStream.rangeClosed(1, numOfThreads).parallel().mapToObj(i -> Futures.getUnchecked(lazyFuture)).collect(toList());
        });

        while (latch.getCount() > 0) {
            latch.countDown();
        }

        assertEquals("All threads receives the same result", Collections.nCopies(numOfThreads, 1), task.get());
    }

}