package org.testcontainers.utility;

public interface ThrowingFunction<T, R> {

    R apply(T t) throws Exception;
}
