package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class MetaAnnotationTest {

    @TcContainer
    private static final PostgreSQLContainer<?> POSTGRESQL = new PostgreSQLContainer<>(
        JUnitJupiterTestImages.POSTGRES_IMAGE
    );

    @Test
    void test() {
        assertThat(POSTGRESQL.isRunning()).isTrue();
    }
}

@Container
@Retention(RetentionPolicy.RUNTIME)
@interface TcContainer {
}
