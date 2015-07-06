package org.rnorth.testcontainers.containers;

import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerInfo;

/**
 * @author richardnorth
 */
public class MySQLContainer extends DatabaseContainer {

    private static final String IMAGE = "mysql";
    private String mySqlPort;

    @Override
    protected void containerIsStarting(ContainerInfo containerInfo) {
        mySqlPort = containerInfo.networkSettings().ports().get("3306/tcp").get(0).hostPort();
    }

    @Override
    protected String getLivenessCheckPort() {
        return mySqlPort;
    }

    @Override
    protected ContainerConfig getContainerConfig() {
        return ContainerConfig.builder()
                    .image(getDockerImageName())
                    .exposedPorts("3306")
                    .env("MYSQL_DATABASE=test", "MYSQL_USER=test", "MYSQL_PASSWORD=test", "MYSQL_ROOT_PASSWORD=test")
                    .cmd("mysqld")
                    .build();
    }

    @Override
    protected String getDockerImageName() {
        return IMAGE + ":" + tag;
    }

    @Override
    public String getName() {
        return "mysql";
    }

    @Override
    public String getDriverClassName() {
        return "com.mysql.jdbc.Driver";
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:mysql://" + dockerHostIpAddress + ":" + mySqlPort + "/test";
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
