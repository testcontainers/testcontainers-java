package org.testcontainers.providers.kubernetes.execution;

import lombok.SneakyThrows;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

public class NullInputStream extends InputStream {

    public CountDownLatch countDownLatch = new CountDownLatch(1);

    @Override
    @SneakyThrows
    public int read() throws IOException {
         countDownLatch.await();
        return -1;
    }


    @Override
    public void close() throws IOException {
        countDownLatch.countDown();
    }
}
