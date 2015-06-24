package org.rnorth.testcontainers.containers;

import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerInfo;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.PoolInitializationException;

/**
 * Created by gusohal on 26/04/15.
 */
public class OracleContainer extends AbstractContainer implements DatabaseContainer  {

    private static final String IMAGE = "alexeiled/docker-oracle-xe-11g";
    private String oraclePort;

    @Override
    protected void containerIsStarting(ContainerInfo containerInfo) {
        oraclePort = containerInfo.networkSettings().ports().get("1521/tcp").get(0).hostPort();
    }

    @Override
    protected String getLivenessCheckPort() {
        return oraclePort;
    }

    @Override
    protected ContainerConfig getContainerConfig() {
        return ContainerConfig.builder()
                .image(getDockerImageName())
                .exposedPorts("1521", "22", "8080")
                .build();
    }

    @Override
    protected String getDockerImageName() {
        return IMAGE + ":" + tag;
    }

    @Override
    public String getName() {
        return "oracle";
    }

    @Override
    public String getDriverClassName() {
        return "oracle.jdbc.OracleDriver";
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:oracle:thin:" + getUsername() + "/" + getPassword() + "@//" + dockerHostIpAddress + ":" + oraclePort + "/xe";
    }

    @Override
    public String getUsername() {
        return "system";
    }

    @Override
    public String getPassword() {
        return "oracle";
    }

    @Override
    public void waitUntilContainerStarted() {
        super.waitUntilContainerStarted();
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(getJdbcUrl());
        hikariConfig.setConnectionTestQuery("SELECT 1 FROM dual");
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setMaximumPoolSize(1);

        for (int i = 0; i < 30000; i++) {
            try {
                new HikariDataSource(hikariConfig);
                return;
            } catch (PoolInitializationException e) {
                try {
                    Thread.sleep(5000L);
                } catch (InterruptedException ignored) {
                }
            }
        }
        throw new IllegalStateException("Timed out waiting for database server to come up");
    }

    @Override
    public String getContainerId() { return containerId; }
}