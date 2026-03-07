package org.testcontainers.jdbc;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This Test class validates that all supported JDBC URL's can be parsed by ConnectionUrl class.
 */
class ConnectionUrlDriversTests {

    public static Stream<Arguments> data() {
        return Stream.of(
            Arguments.arguments(
                "jdbc:tc:mysql:8.0.36://hostname/test",
                "mysql",
                Optional.of("8.0.36"),
                "hostname/test",
                "test"
            ),
            Arguments.arguments("jdbc:tc:mysql://hostname/test", "mysql", Optional.empty(), "hostname/test", "test"),
            Arguments.arguments(
                "jdbc:tc:postgresql:1.2.3://hostname/test",
                "postgresql",
                Optional.of("1.2.3"),
                "hostname/test",
                "test"
            ),
            Arguments.arguments(
                "jdbc:tc:postgresql://hostname/test",
                "postgresql",
                Optional.empty(),
                "hostname/test",
                "test"
            ),
            Arguments.arguments(
                "jdbc:tc:sqlserver:1.2.3://localhost;instance=SQLEXPRESS:1433;databaseName=test",
                "sqlserver",
                Optional.of("1.2.3"),
                "localhost;instance=SQLEXPRESS:1433;databaseName=test",
                "test"
            ),
            Arguments.arguments(
                "jdbc:tc:sqlserver://localhost;instance=SQLEXPRESS:1433;databaseName=test",
                "sqlserver",
                Optional.empty(),
                "localhost;instance=SQLEXPRESS:1433;databaseName=test",
                "test"
            ),
            Arguments.arguments(
                "jdbc:tc:mariadb:1.2.3://localhost:3306/test",
                "mariadb",
                Optional.of("1.2.3"),
                "localhost:3306/test",
                "test"
            ),
            Arguments.arguments(
                "jdbc:tc:mariadb://localhost:3306/test",
                "mariadb",
                Optional.empty(),
                "localhost:3306/test",
                "test"
            ),
            Arguments.arguments(
                "jdbc:tc:oracle:1.2.3:thin://@localhost:1521/test",
                "oracle",
                Optional.of("1.2.3"),
                "localhost:1521/test",
                "test"
            ),
            Arguments.arguments(
                "jdbc:tc:oracle:1.2.3:thin:@localhost:1521/test",
                "oracle",
                Optional.of("1.2.3"),
                "localhost:1521/test",
                "test"
            ),
            Arguments.arguments(
                "jdbc:tc:oracle:thin:@localhost:1521/test",
                "oracle",
                Optional.empty(),
                "localhost:1521/test",
                "test"
            ),
            Arguments.arguments(
                "jdbc:tc:oracle:1.2.3:thin:@localhost:1521:test",
                "oracle",
                Optional.of("1.2.3"),
                "localhost:1521:test",
                "test"
            ),
            Arguments.arguments(
                "jdbc:tc:oracle:1.2.3:thin://@localhost:1521:test",
                "oracle",
                Optional.of("1.2.3"),
                "localhost:1521:test",
                "test"
            ),
            Arguments.arguments(
                "jdbc:tc:oracle:1.2.3-anything:thin://@localhost:1521:test",
                "oracle",
                Optional.of("1.2.3-anything"),
                "localhost:1521:test",
                "test"
            ),
            Arguments.arguments(
                "jdbc:tc:oracle:thin:@localhost:1521:test",
                "oracle",
                Optional.empty(),
                "localhost:1521:test",
                "test"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("data")
    void test(String jdbcUrl, String databaseType, Optional<String> tag, String dbHostString, String databaseName) {
        ConnectionUrl url = ConnectionUrl.newInstance(jdbcUrl);
        assertThat(url.getDatabaseType()).as("Database Type is as expected").isEqualTo(databaseType);
        assertThat(url.getImageTag()).as("Image tag is as expected").isEqualTo(tag);
        assertThat(url.getDbHostString()).as("Database Host String is as expected").isEqualTo(dbHostString);
        assertThat(url.getDatabaseName().orElse("")).as("Database Name is as expected").isEqualTo(databaseName);
    }
}
