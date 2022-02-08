package org.testcontainers.junit.mssqlserver;

import org.junit.Test;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConnectionParamMSSQLServerTest {

    @Test
    public void turnOffEncryptByDefaultInJDBCUrl() {
        // given: An instance of MS SQL Server container
        try (MSSQLServerContainer<?> mssqlServerContainer = new MSSQLServerContainer<>(DockerImageName.parse("mcr.microsoft.com/mssql/server").withTag("2017-CU12"))) {
            mssqlServerContainer.start();
            // expect: getting JDBC url will contain param for turning off encrypt
            assertTrue(
                "The JDBC driver of MS SQL Server enables encryption by default for versions > 10.1.0. " +
                    "We need to disable it by default to be able to use the container without having to pass extra params.",
                mssqlServerContainer.getJdbcUrl().contains("encrypt=false")
            );
            assertFalse(mssqlServerContainer.getJdbcUrl().contains("encrypt=true"));
        }
    }
}
