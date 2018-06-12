package org.testcontainers.containers;

import static java.time.temporal.ChronoUnit.SECONDS;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

public class SybaseContainer<SELF extends SybaseContainer<SELF>> extends JdbcDatabaseContainer<SELF> {
    public static final String NAME = "sybase";
    public static final String IMAGE = "nguoianphu/docker-sybase";
    public static final String DEFAULT_TAG = "latest";

    public static final Integer SYBASE_PORT = 5000;
    private String databaseName = "master";
    private String username = "sa";
    private String password = "myPassword";

    public SybaseContainer() {
        this(IMAGE + ":" + DEFAULT_TAG);
    }

    public SybaseContainer(final String dockerImageName) {
        super(dockerImageName);
        this.waitStrategy = new LogMessageWaitStrategy()
                .withRegEx(".*PCI Bridge Config: Finished the configuration.*\\n")
                .withTimes(2)
                .withStartupTimeout(Duration.of(30, SECONDS));
    }

    @NotNull
    @Override
    protected Set<Integer> getLivenessCheckPorts() {
        return new HashSet<>(getMappedPort(SYBASE_PORT));
    }

    @Override
    protected void configure() {

        addExposedPort(SYBASE_PORT);
    }

    @Override
    public String getDriverClassName() {
        return "net.sourceforge.jtds.jdbc.Driver";
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:jtds:sybase://" + getContainerIpAddress() + ":" + getMappedPort(SYBASE_PORT) + "/" + databaseName;
    }

    @Override
    public String getDatabaseName() {
        return databaseName;
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
    public String getTestQueryString() {
        return "SELECT 1";
    }

    @Override
    protected void waitUntilContainerStarted() {
        getWaitStrategy().waitUntilReady(this);
    }
}
