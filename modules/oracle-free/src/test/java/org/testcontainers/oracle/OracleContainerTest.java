package org.testcontainers.oracle;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OracleContainerTest {

    @Test
    void acceptsOracleXeImage() {
        assertThatCode(() -> new OracleContainer("gvenzl/oracle-xe:21-slim-faststart")).doesNotThrowAnyException();
    }

    @Test
    void rejectsUnrelatedImage() {
        assertThatThrownBy(() -> new OracleContainer("postgres:17")).isInstanceOf(IllegalStateException.class);
    }
}
