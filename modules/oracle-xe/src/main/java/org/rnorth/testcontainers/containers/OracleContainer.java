package org.rnorth.testcontainers.containers;

import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerInfo;
import oracle.jdbc.pool.OracleDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author gusohal
 */
public class OracleContainer extends AbstractContainer implements DatabaseContainer {

    private static final String IMAGE = "wnameless/oracle-xe-11g";
    private String sshPort;
    private String oraclePort;
    private String webPort;

    @Override
    protected void containerIsStarting(ContainerInfo containerInfo) {
        sshPort = containerInfo.networkSettings().ports().get("22/tcp").get(0).hostPort();
        oraclePort = containerInfo.networkSettings().ports().get("1521/tcp").get(0).hostPort();
        webPort = containerInfo.networkSettings().ports().get("8080/tcp").get(0).hostPort();
    }

    @Override
    protected String getLivenessCheckPort() {
        return oraclePort;
    }

    @Override
    protected ContainerConfig getContainerConfig() {
        return ContainerConfig.builder()
                .image(getDockerImageName())
                .exposedPorts("22", "1521", "8080")
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
        return "jdbc:oracle:thin:" + getUsername() + "/" + getPassword() + "@//" + dockerHostIpAddress + ":" + oraclePort + "/" + getSid();
    }

    @Override
    public String getUsername() {
        return "system";
    }

    @Override
    public String getPassword() {
        return "oracle";
    }

    public String getSid() {
        return "xe";
    }

    @Override
    public void waitUntilContainerStarted() {
        super.waitUntilContainerStarted();

        OracleDataSource dataSource;

        for (int i = 0; i < 600; i++) {
            try {
                dataSource = new OracleDataSource();
                dataSource.setURL(getJdbcUrl());
                Connection connection = dataSource.getConnection();

                boolean success = connection.createStatement().execute("SELECT 1 FROM DUAL");

                if (success) {
                    return;
                }
            } catch (SQLException e) {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException ignored) {
                }
            }
        }

        throw new IllegalStateException("Timed out waiting for database server to come up");
    }

    @Override
    public String getContainerId() {
        return containerId;
    }

    public String getSshPort() {
        return sshPort;
    }

    public String getOraclePort() {
        return oraclePort;
    }

    public String getWebPort() {
        return webPort;
    }
}