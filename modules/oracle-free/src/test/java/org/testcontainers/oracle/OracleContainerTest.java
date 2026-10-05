package org.testcontainers.oracle;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OracleContainerTest {

    @Test
    void acceptsOracleXeImage() {
        assertThatCode(() -> new OracleContainer("gvenzl/oracle-xe:21-slim-faststart")).doesNotThrowAnyException();
    }

    @Test
    void usesOracleXeConnectionDefaults() {
        final OracleContainer container = new FixedAddressOracleContainer("gvenzl/oracle-xe:21-slim-faststart");

        assertThat(container.getDatabaseName()).isEqualTo("xepdb1");
        assertThat(container.getSid()).isEqualTo("xe");
        assertThat(container.getJdbcUrl()).isEqualTo("jdbc:oracle:thin:@localhost:15210/xepdb1");
        assertThat(container.usingSid().getJdbcUrl()).isEqualTo("jdbc:oracle:thin:@localhost:15210:xe");
    }

    @Test
    void keepsOracleFreeConnectionDefaults() {
        final OracleContainer container = new FixedAddressOracleContainer("gvenzl/oracle-free:slim");

        assertThat(container.getDatabaseName()).isEqualTo("freepdb1");
        assertThat(container.getSid()).isEqualTo("free");
        assertThat(container.getJdbcUrl()).isEqualTo("jdbc:oracle:thin:@localhost:15210/freepdb1");
        assertThat(container.usingSid().getJdbcUrl()).isEqualTo("jdbc:oracle:thin:@localhost:15210:free");
    }

    @Test
    void preservesExplicitDatabaseNameForOracleXe() {
        final OracleContainer container = new FixedAddressOracleContainer("gvenzl/oracle-xe:21-slim-faststart")
            .withDatabaseName("custompdb");

        assertThat(container.getDatabaseName()).isEqualTo("custompdb");
        assertThat(container.getJdbcUrl()).isEqualTo("jdbc:oracle:thin:@localhost:15210/custompdb");
    }

    @Test
    void rejectsUnrelatedImage() {
        assertThatThrownBy(() -> new OracleContainer("postgres:17")).isInstanceOf(IllegalStateException.class);
    }

    private static final class FixedAddressOracleContainer extends OracleContainer {

        private FixedAddressOracleContainer(String dockerImageName) {
            super(dockerImageName);
        }

        @Override
        public String getHost() {
            return "localhost";
        }

        @Override
        public Integer getMappedPort(int originalPort) {
            return 15210;
        }
    }
}
