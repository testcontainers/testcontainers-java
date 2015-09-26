package org.testcontainers.containers;

import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerInfo;

/**
 * @author richardnorth
 */
public class MySQLContainer extends JdbcDatabaseContainer {

    public static final String NAME = "mysql";
    public static final String IMAGE = "mysql";
    public static final String MY_CNF_CONFIG_OVERRIDE_PARAM_NAME = "TC_MY_CNF";
    private String mySqlPort;

    public MySQLContainer() {
        super(IMAGE + ":latest");
    }

    public MySQLContainer(String dockerImageName) {
        super(dockerImageName);
    }

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
        optionallyMapResourceParameterAsVolume(MY_CNF_CONFIG_OVERRIDE_PARAM_NAME, "/etc/mysql/conf.d");

        withExposedPorts(3306);
        withEnv("MYSQL_DATABASE", "test");
        withEnv("MYSQL_USER", "test");
        withEnv("MYSQL_PASSWORD", "test");
        withEnv("MYSQL_ROOT_PASSWORD", "test");
        withCommand("mysqld");

        return super.getContainerConfig();

//        return ContainerConfig.builder()
//                    .image(getDockerImageName())
//                    .exposedPorts("3306")
//                    .env("MYSQL_DATABASE=test", "MYSQL_USER=test", "MYSQL_PASSWORD=test", "MYSQL_ROOT_PASSWORD=test")
//                    .cmd("mysqld")
//                    .build();
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
        return "jdbc:mysql://" + getIpAddress() + ":" + mySqlPort + "/test";
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

    public MySQLContainer withConfigurationOverride(String s) {
        parameters.put(MY_CNF_CONFIG_OVERRIDE_PARAM_NAME, s);
        return this;
    }
}
