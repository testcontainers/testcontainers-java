import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerInfo;

/**
 * @author richardnorth
 */
public class MySQLContainerRule extends AbstractContainerRule {

    private static final String MYSQL_IMAGE = "mysql:5.6.23";
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
        return MYSQL_IMAGE;
    }

    public String getJdbcUrl() {
        return "jdbc:mysql://" + dockerHostIpAddress + ":" + mySqlPort + "/test";
    }

    public String getUsername() {
        return "test";
    }

    public String getPassword() {
        return "test";
    }
}
