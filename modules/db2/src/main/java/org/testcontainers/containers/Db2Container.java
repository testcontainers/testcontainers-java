package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.LicenseAcceptance;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

public class Db2Container extends JdbcDatabaseContainer<Db2Container> {

    public static final String NAME = "db2";
    public static final String DEFAULT_DB2_IMAGE_NAME = "ibmcom/db2";
    public static final String DEFAULT_TAG = "11.5.0.0a";
    public static final String DEFAULT_DOCKER_IMAGE_NAME = DEFAULT_DB2_IMAGE_NAME + ":" + DEFAULT_TAG;
    public static final int DB2_PORT = 50000;

    public static final String DEFAULT_DATABASE_NAME = "test";
    public static final String DEFAULT_USERNAME = "db2inst1";
    public static final String DEFAULT_PASSWORD = "foobar1234";

    private String databaseName = DEFAULT_DATABASE_NAME;
    private String username = DEFAULT_USERNAME;
    private String password = DEFAULT_PASSWORD;

    public Db2Container() {
        this(DEFAULT_DOCKER_IMAGE_NAME);
    }

    public Db2Container(String imageName) {
        super(imageName);
        withPrivilegedMode(true);
        this.waitStrategy = new LogMessageWaitStrategy()
                .withRegEx(".*Setup has completed\\..*")
                .withStartupTimeout(Duration.of(10, ChronoUnit.MINUTES));
    }

    @Override
    protected Set<Integer> getLivenessCheckPorts() {
        return new HashSet<>(getMappedPort(DB2_PORT));
    }

    @Override
    protected void configure() {
        // If license was not accepted programatically, check if it was accepted via resource file
        if (!getEnvMap().containsKey("LICENSE")) {
            LicenseAcceptance.assertLicenseAccepted(this.getDockerImageName());
            acceptLicense();
        }

        addExposedPort(DB2_PORT);

        addEnv("DBNAME", databaseName);
        addEnv("DB2INSTANCE", username);
        addEnv("DB2INST1_PASSWORD", password);

        // These settings help the DB2 container start faster
        if (!getEnvMap().containsKey("AUTOCONFIG"))
            addEnv("AUTOCONFIG", "false");
        if (!getEnvMap().containsKey("ARCHIVE_LOGS"))
            addEnv("ARCHIVE_LOGS", "false");
    }

    /**
     * Accepts the license for the DB2 container by setting the LICENSE=accept
     * variable as described at <a href="https://hub.docker.com/r/ibmcom/db2">https://hub.docker.com/r/ibmcom/db2</a>
     */
    public Db2Container acceptLicense() {
        addEnv("LICENSE", "accept");
        return this;
    }

    @Override
    public String getDriverClassName() {
        return "com.ibm.db2.jcc.DB2Driver";
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:db2://" + getContainerIpAddress() + ":" + getMappedPort(DB2_PORT) + "/" + databaseName;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getDatabaseName() {
        return databaseName;
    }

    @Override
    public Db2Container withUsername(String username) {
        this.username = username;
        return this;
    }

    @Override
    public Db2Container withPassword(String password) {
        this.password = password;
        return this;
    }

    @Override
    public Db2Container withDatabaseName(String dbName) {
        this.databaseName = dbName;
        return this;
    }

    @Override
    protected void waitUntilContainerStarted() {
        getWaitStrategy().waitUntilReady(this);
    }

    @Override
    protected String getTestQueryString() {
        return "SELECT 1 FROM SYSIBM.SYSDUMMY1";
    }
}
