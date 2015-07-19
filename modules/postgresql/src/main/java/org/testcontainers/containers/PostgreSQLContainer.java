package org.testcontainers.containers;

import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerInfo;

/**
 * @author richardnorth
 */
public class PostgreSQLContainer extends JdbcDatabaseContainer {

    private static final String IMAGE = "postgres";
    private String postgresPort;

    @Override
    protected void containerIsStarting(ContainerInfo containerInfo) {
        postgresPort = containerInfo.networkSettings().ports().get("5432/tcp").get(0).hostPort();
    }

    @Override
    protected String getLivenessCheckPort() {
        return postgresPort;
    }

    @Override
    protected ContainerConfig getContainerConfig() {
        return ContainerConfig.builder()
                    .image(getDockerImageName())
                    .exposedPorts("5432")
                    .env("POSTGRES_DATABASE=test", "POSTGRES_USER=test", "POSTGRES_PASSWORD=test")
                    .cmd("postgres")
                    .build();
    }

    @Override
    protected String getDockerImageName() {
        return IMAGE + ":" + tag;
    }

    @Override
    public String getName() {
        return "postgresql";
    }

    @Override
    public String getDriverClassName() {
        return "org.postgresql.Driver";
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:postgresql://" + dockerHostIpAddress + ":" + postgresPort + "/test";
    }

    @Override
    public String getUsername() {
        return "test";
    }

    @Override
    public String getPassword() {
        return "test";
    }

    @Override
    public String getTestQueryString() {
        return "SELECT 1";
    }
}
