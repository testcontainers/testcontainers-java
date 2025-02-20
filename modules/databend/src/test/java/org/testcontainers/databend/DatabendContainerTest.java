package org.testcontainers.databend;

import org.junit.Test;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class DatabendContainerTest extends AbstractContainerDatabaseTest {

    @Test
    public void testSimple() throws SQLException {
        try (DatabendContainer databend = new DatabendContainer("datafuselabs/databend:v1.2.615")) {
            databend.start();

            ResultSet resultSet = performQuery(databend, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).isEqualTo(1);
        }
    }

    @Test
    public void customCredentialsWithUrlParams() throws SQLException {
        try (
            DatabendContainer databend = new DatabendContainer("datafuselabs/databend:v1.2.615")
                .withUsername("test")
                .withPassword("test")
                .withUrlParam("ssl", "false")
        ) {
            databend.start();

            ResultSet resultSet = performQuery(databend, "SELECT 1;");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).isEqualTo(1);
        }
    }

    @Test
    public void testInsertIntoWithMinioEnabled() throws SQLException {
        try (DatabendContainer databend = new DatabendContainer("datafuselabs/databend:v1.2.615")
                .withMinioEnabled(true)
                .withUsername("test")
                .withPassword("test")
                .withUrlParam("ssl", "false")
        ) {
            databend.start();

            performQuery(databend, "CREATE TABLE test_table (a int, b int);");
            performQuery(databend, "INSERT INTO test_table VALUES (1, 2);");

            ResultSet resultSet = performQuery(databend, "SELECT * FROM test_table;");

            resultSet.next();
            assertThat(resultSet.getInt(1)).isEqualTo(1);
            assertThat(resultSet.getInt(2)).isEqualTo(2);
        }
    }
}
