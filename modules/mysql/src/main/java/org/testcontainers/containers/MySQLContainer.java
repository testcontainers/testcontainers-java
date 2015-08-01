package org.testcontainers.containers;

import com.google.common.io.Files;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.HostConfig;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

/**
 * @author richardnorth
 */
public class MySQLContainer extends JdbcDatabaseContainer {

    public static final String NAME = "mysql";
    public static final String IMAGE = "mysql";
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
    protected void customizeHostConfigBuilder(HostConfig.Builder hostConfigBuilder) {
        if (parameters.containsKey("TC_MY_CNF")) {
            String resourceName = parameters.get("TC_MY_CNF");
            URL classPathResource = ClassLoader.getSystemClassLoader().getResource(resourceName);
            if (classPathResource == null) {
                throw new ContainerLaunchException("Could not locate a classpath resource for TC_MY_CNF of " + resourceName);
            }

            try {
                Path tempVolume = createVolumeDirectory(true);
                String pathToMyIni = classPathResource.getPath();
                Files.copy(new File(pathToMyIni), tempVolume.resolve("overrides.cnf").toFile());
                addFileSystemBind(tempVolume.toFile().getAbsolutePath(), "/etc/mysql/conf.d", BindMode.READ_ONLY);
            } catch (IOException e) {
                throw new ContainerLaunchException("Could not create a temporary volume directory to hold custom my.cnf overrides", e);
            }
        }

        super.customizeHostConfigBuilder(hostConfigBuilder);
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
