package org.testcontainers.containers;


import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

public class SqlEdgeContainerTest {
    @Rule
    public SqlEdgeContainer sqlEdgeContainer = new SqlEdgeContainer(
        DockerImageName.parse("mcr.microsoft.com/azure-sql-edge:latest")
    );

    @Test
    public void testWithJdbc() {
        String host = sqlEdgeContainer.getHost();
    }
}
