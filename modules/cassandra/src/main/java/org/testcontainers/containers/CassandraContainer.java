package org.testcontainers.containers;

import com.datastax.driver.core.Cluster;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.delegate.CassandraDatabaseDelegate;
import org.testcontainers.delegate.DatabaseDelegate;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.utility.MountableFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Cassandra container
 *
 * Supports 2.x and 3.x Cassandra versions
 *
 * @author Eugeny Karpov
 */
public class CassandraContainer<SELF extends CassandraContainer<SELF>> extends GenericContainer<SELF> {

    public static final String IMAGE = "cassandra";
    public static final Integer CQL_PORT = 9042;
    private static final String CONTAINER_CONFIG_LOCATION = "/etc/cassandra";
    private static final String USERNAME = "cassandra";
    private static final String PASSWORD = "cassandra";

    private String configLocation;
    private List<String> initScriptPaths;
    private boolean enableJmxReporting;

    public CassandraContainer() {
        this(IMAGE + ":3.11.2");
    }

    public CassandraContainer(String dockerImageName) {
        super(dockerImageName);
        addExposedPort(CQL_PORT);
        setStartupAttempts(3);
        this.enableJmxReporting = false;
        this.initScriptPaths = new ArrayList<>();
    }

    @Override
    protected void configure() {
        optionallyMapResourceParameterAsVolume(CONTAINER_CONFIG_LOCATION, configLocation);
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        runInitScriptsIfRequired();
    }

    /**
     * Apply all configured init scripts to the running database
     */
    private void runInitScriptsIfRequired() {
        for (String initScriptPath : initScriptPaths) {
            ScriptUtils.runInitScript(getDatabaseDelegate(), initScriptPath);
        }
    }

    /**
     * Map (effectively replace) directory in Docker with the content of resourceLocation if resource location is not null
     *
     * Protected to allow for changing implementation by extending the class
     *
     * @param pathNameInContainer path in docker
     * @param resourceLocation    relative classpath to resource
     */
    protected void optionallyMapResourceParameterAsVolume(String pathNameInContainer, String resourceLocation) {
        Optional.ofNullable(resourceLocation)
                .map(MountableFile::forClasspathResource)
                .ifPresent(mountableFile -> withCopyFileToContainer(mountableFile, pathNameInContainer));
    }

    /**
     * Initialize Cassandra with the custom overridden Cassandra configuration
     * <p>
     * Be aware, that Docker effectively replaces all /etc/cassandra content with the content of config location, so if
     * Cassandra.yaml in configLocation is absent or corrupted, then Cassandra just won't launch
     *
     * @param configLocation relative classpath with the directory that contains cassandra.yaml and other configuration files
     */
    public SELF withConfigurationOverride(String configLocation) {
        this.configLocation = configLocation;
        return self();
    }

    /**
     * Initialize Cassandra with one or more init CQL scripts.
     * <p>
     * The CQL scripts will be applied after container is started (see using WaitStrategy)
     *
     * @param initScriptPath relative classpath resource
     */
    public SELF withInitScript(String initScriptPath, String... extraInitScriptPaths) {
        this.initScriptPaths = new ArrayList<>();
        this.initScriptPaths.add(initScriptPath);
        this.initScriptPaths.addAll(Arrays.asList(extraInitScriptPaths));
        return self();
    }

    /**
     * Initialize Cassandra client with JMX reporting enabled or disabled
     */
    public SELF withJmxReporting(boolean enableJmxReporting) {
        this.enableJmxReporting = enableJmxReporting;
        return self();
    }

    /**
     * Get username
     *
     * By default Cassandra has authenticator: AllowAllAuthenticator in cassandra.yaml
     * If username and password need to be used, then authenticator should be set as PasswordAuthenticator
     * (through custom Cassandra configuration) and through CQL with default cassandra-cassandra credentials
     * user management should be modified
     */
    public String getUsername() {
        return USERNAME;
    }

    /**
     * Get password
     *
     * By default Cassandra has authenticator: AllowAllAuthenticator in cassandra.yaml
     * If username and password need to be used, then authenticator should be set as PasswordAuthenticator
     * (through custom Cassandra configuration) and through CQL with default cassandra-cassandra credentials
     * user management should be modified
     */
    public String getPassword() {
        return PASSWORD;
    }

    /**
     * Get configured Cluster
     *
     * Can be used to obtain connections to Cassandra in the container
     */
    public Cluster getCluster() {
        return getCluster(this, enableJmxReporting);
    }

    public static Cluster getCluster(ContainerState containerState, boolean enableJmxReporting) {
        final Cluster.Builder builder = Cluster.builder()
            .addContactPoint(containerState.getHost())
            .withPort(containerState.getMappedPort(CQL_PORT));
        if (!enableJmxReporting) {
            builder.withoutJMXReporting();
        }
        return builder.build();
    }

    public static Cluster getCluster(ContainerState containerState) {
        return getCluster(containerState, false);
    }

    private DatabaseDelegate getDatabaseDelegate() {
        return new CassandraDatabaseDelegate(this);
    }
}
