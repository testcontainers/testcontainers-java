package org.testcontainers.dockerclient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@ResourceLock("jvm.defaultUncaughtExceptionHandler")
@ResourceLock("testcontainers.shaded.awaitility")
class DockerClientProviderStrategyTest {

    @Test
    void testDoesNotModifyGlobalUncaughtExceptionHandler() throws Exception {
        Class<?> awaitilityClass = Class.forName("org.testcontainers.shaded.org.awaitility.Awaitility");
        Class<?> listenerClass = Class.forName(
            "org.testcontainers.shaded.org.awaitility.core.ConditionEvaluationListener"
        );
        Method setListener = awaitilityClass.getMethod("setDefaultConditionEvaluationListener", listenerClass);

        AtomicReference<Thread.UncaughtExceptionHandler> observed = new AtomicReference<>();
        Object proxy = Proxy.newProxyInstance(
            listenerClass.getClassLoader(),
            new Class<?>[] { listenerClass },
            (p, method, args) -> {
                if (method.getName().equals("conditionEvaluated")) {
                    observed.compareAndSet(null, Thread.getDefaultUncaughtExceptionHandler());
                }
                return null;
            }
        );

        Thread.UncaughtExceptionHandler original = Thread.getDefaultUncaughtExceptionHandler();
        Thread.UncaughtExceptionHandler sentinel = (thread, throwable) -> {};

        try {
            setListener.invoke(null, proxy);
            Thread.setDefaultUncaughtExceptionHandler(sentinel);

            boolean result;
            try (ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
                int port = serverSocket.getLocalPort();

                DockerClientProviderStrategy strategy = new DockerClientProviderStrategy() {
                    @Override
                    public String getDescription() {
                        return "test strategy";
                    }

                    @Override
                    public TransportConfig getTransportConfig() {
                        return TransportConfig.builder().dockerHost(URI.create("tcp://127.0.0.1:" + port)).build();
                    }
                };

                result = strategy.test();
            }

            assertThat(result).isTrue();
            assertThat(observed.get()).as("shaded Awaitility condition listener should have executed").isNotNull();
            assertThat(observed.get())
                .as("Docker strategy testing must not replace the JVM default uncaught exception handler")
                .isSameAs(sentinel);
        } finally {
            setListener.invoke(null, new Object[] { null });
            Thread.setDefaultUncaughtExceptionHandler(original);
        }
    }
}
