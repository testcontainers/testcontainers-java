package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.ComparableVersion;
import org.testcontainers.utility.DockerImageName;

import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

/**
 * Testcontainers implementation for Solr.
 * <p>
 * Supported image: {@code solr}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>Solr: 8983</li>
 *     <li>Zookeeper: 9983</li>
 * </ul>
 */
public class SolrContainer extends GenericContainer<SolrContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("solr");

    @Deprecated
    public static final String IMAGE = DEFAULT_IMAGE_NAME.getUnversionedPart();

    @Deprecated
    public static final String DEFAULT_TAG = "8.3.0";

    public static final Integer ZOOKEEPER_PORT = 9983;

    public static final Integer SOLR_PORT = 8983;

    private SolrContainerConfiguration configuration;

    private final ComparableVersion imageVersion;

    /**
     * @deprecated use {@link #SolrContainer(DockerImageName)} instead
     */
    @Deprecated
    public SolrContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    /**
     * @deprecated use {@link #SolrContainer(DockerImageName)} instead
     */
    public SolrContainer(final String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public SolrContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        this.waitStrategy =
            new LogMessageWaitStrategy()
                .withRegEx(".*o\\.e\\.j\\.s\\.Server Started.*")
                .withStartupTimeout(Duration.of(60, ChronoUnit.SECONDS));
        this.configuration = new SolrContainerConfiguration();
        this.imageVersion = new ComparableVersion(dockerImageName.getVersionPart());
    }

    public SolrContainer withZookeeper(boolean zookeeper) {
        configuration.setZookeeper(zookeeper);
        return self();
    }

    public SolrContainer withCollection(String collection) {
        if (StringUtils.isEmpty(collection)) {
            throw new IllegalArgumentException("Collection name must not be empty");
        }
        configuration.setCollectionName(collection);
        return self();
    }

    public SolrContainer withConfiguration(String name, URL solrConfig) {
        if (StringUtils.isEmpty(name) || solrConfig == null) {
            throw new IllegalArgumentException();
        }
        configuration.setConfigurationName(name);
        configuration.setSolrConfiguration(solrConfig);
        return self();
    }

    public SolrContainer withSchema(URL schema) {
        configuration.setSolrSchema(schema);
        return self();
    }

    public int getSolrPort() {
        return getMappedPort(SOLR_PORT);
    }

    public int getZookeeperPort() {
        return getMappedPort(ZOOKEEPER_PORT);
    }

    @Override
    @SneakyThrows
    protected void configure() {
        if (configuration.getSolrSchema() != null && configuration.getSolrConfiguration() == null) {
            throw new IllegalStateException("Solr needs to have a configuration if you want to use a schema");
        }
        // Generate Command Builder
        String command = "solr start -f";
        // Add Default Ports
        this.addExposedPort(SOLR_PORT);

        // Configure Zookeeper
        if (configuration.isZookeeper()) {
            this.addExposedPort(ZOOKEEPER_PORT);
            if (this.imageVersion.isGreaterThanOrEqualTo("9.7.0")) {
                command = "-DzkRun --host localhost";
            } else {
                command = "-DzkRun -h localhost";
            }
        }

        // Apply generated Command
        this.setCommand(command);
    }

    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {
        return new HashSet<>(getSolrPort());
    }

    @Override
    protected void waitUntilContainerStarted() {
        getWaitStrategy().waitUntilReady(this);
    }

    @Override
    @SneakyThrows
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        if (!configuration.isZookeeper()) {
            ExecResult result = execInContainer("solr", "create", "-c", configuration.getCollectionName());
            if (result.getExitCode() != 0) {
                throw new IllegalStateException(
                    "Unable to create solr core:\nStdout: " + result.getStdout() + "\nStderr:" + result.getStderr()
                );
            }
            return;
        }

        if (StringUtils.isNotEmpty(configuration.getConfigurationName())) {
            SolrClientUtils.uploadConfiguration(
                getHost(),
                getSolrPort(),
                configuration.getConfigurationName(),
                configuration.getSolrConfiguration(),
                configuration.getSolrSchema()
            );
        }

        SolrClientUtils.createCollection(
            getHost(),
            getSolrPort(),
            configuration.getCollectionName(),
            configuration.getConfigurationName()
        );
    }
}
