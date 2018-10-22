package org.testcontainers.utility;

import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DockerLoggerFactory {
    private static final ConcurrentMap<String, Logger> cache = new ConcurrentHashMap<>();

    public static Logger getLogger(String dockerImageName) {
        return getLogger("org.testcontainers", dockerImageName);
    }

    public static Logger getLogger(Class<?> source, String dockerImageName) {
        return getLogger(source.getName(), dockerImageName);
    }

    public static Logger getLogger(String source, String dockerImageName) {
        return cache.computeIfAbsent(source + "/" + dockerImageName, name -> {
            final String abbreviatedName;
            if (dockerImageName.contains("@sha256")) {
                abbreviatedName = dockerImageName.substring(0, dockerImageName.indexOf("@sha256") + 14) + "...";
            } else {
                abbreviatedName = dockerImageName;
            }

            final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            final Logger logger = LoggerFactory.getLogger(source);

            return (Logger) Proxy.newProxyInstance(
                classLoader,
                new Class[]{
                    Logger.class
                },
                (proxy, method, args) -> {
                    try (MDC.MDCCloseable c = MDC.putCloseable("image.name", abbreviatedName)) {
                        return method.invoke(logger, args);
                    }
                }
            );
        });
    }
}
