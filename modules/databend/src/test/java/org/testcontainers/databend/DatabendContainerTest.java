package org.testcontainers.databend;

import org.junit.jupiter.api.Test;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.SQLException;

class DatabendContainerTest extends AbstractContainerDatabaseTest {

    @Test
    void testSimple() throws SQLException {
        try ( // container {
            DatabendContainer databend = new DatabendContainer("datafuselabs/databend:v1.2.615")
            // }
        ) {
            databend.start();

            executeSelectOneQuery(databend);
        }
    }

    @Test
    void customCredentialsWithUrlParams() throws SQLException {
        try (
            DatabendContainer databend = new DatabendContainer("datafuselabs/databend:v1.2.615")
                .withUsername("test")
                .withPassword("test")
                .withUrlParam("ssl", "false")
        ) {
            databend.start();

            executeSelectOneQuery(databend);
        }
    }
}
