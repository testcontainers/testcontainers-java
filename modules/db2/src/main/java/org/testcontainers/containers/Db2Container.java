package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.utility.LogUtils;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

public class Db2Container<SELF extends Db2Container<SELF>> extends JdbcDatabaseContainer<SELF> {

    static final String NAME = "db2";
    static final String DEFAULT_DB2_IMAGE_NAME = "ibmcom/db2express-c";
    static final String DEFAULT_TAG = "10.5.0.5-3.10.0";
    private static final int DB2_PORT = 50000;

    private String username = "db2inst1";
    private String password = "foobar1234";

    private static final int DEFAULT_STARTUP_TIMEOUT_SECONDS = 240;
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 240;

    public Db2Container() {
        this(DEFAULT_DB2_IMAGE_NAME + ":" + DEFAULT_TAG);
    }

    public Db2Container(String imageName) {
        super(imageName);
        withStartupTimeoutSeconds(DEFAULT_STARTUP_TIMEOUT_SECONDS);
        withConnectTimeoutSeconds(DEFAULT_CONNECT_TIMEOUT_SECONDS);
    }

    @Override
    protected void configure() {
        addExposedPort(DB2_PORT);

        addEnv("LICENSE", "accept"); // TODO: explicit?
        addEnv("DB2INST1_PASSWORD", password);

        withCommand("db2start");

    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        super.containerIsStarting(containerInfo);

        WaitingConsumer waitingConsumer = new WaitingConsumer();
        LogUtils.followOutput(DockerClientFactory.instance().client(), containerInfo.getId(), waitingConsumer);

        Predicate<OutputFrame> waitPredicate = outputFrame ->
            outputFrame.getUtf8String()
                .matches("(?s).*DB2START processing was successful.*");


        try {
            waitingConsumer.waitUntil(waitPredicate);
            execInContainer("/bin/sh", "-c", "runuser -l db2inst1 -c 'db2 create db tc'");
        } catch (TimeoutException e) {
            throw new ContainerLaunchException("Timeout while waiting for db started log message");
        } catch (InterruptedException | IOException e) {
            throw new ContainerLaunchException("Error while creating database");
        }

    }

    @Override
    public String getDriverClassName() {
        return "com.ibm.db2.jcc.DB2Driver";
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:db2://" + getContainerIpAddress() + ":" + getMappedPort(DB2_PORT) + "/tc";
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
    protected String getTestQueryString() {
        return "SELECT 1 FROM SYSIBM.SYSDUMMY1";
    }
}
