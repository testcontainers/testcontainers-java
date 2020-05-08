package org.testcontainers.containers;

import static java.time.temporal.ChronoUnit.SECONDS;

import java.net.URL;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import javax.naming.ConfigurationException;

import com.github.dockerjava.api.command.InspectContainerResponse;

import lombok.SneakyThrows;

import org.apache.commons.lang.StringUtils;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

/**
 * SolrContainer allows a solr container to be launched and controlled.
 *
 * @author Simon Schneider
 */
public class SolrContainer<SELF extends SolrContainer<SELF>> extends GenericContainer<SELF> {

    public static final String IMAGE = "solr";
    public static final String DEFAULT_TAG = "8.3.0";

    public static final Integer ZOOKEEPER_PORT = 9983;
    public static final Integer SOLR_PORT = 8983;

    private SolrContainerConfiguration configuration;

    public SolrContainer() {
        this(IMAGE + ":" + DEFAULT_TAG);
    }

    public SolrContainer(final String dockerImageName) {
        super(dockerImageName);
        this.waitStrategy = new LogMessageWaitStrategy()
            .withRegEx(".*o\\.e\\.j\\.s\\.Server Started.*")
            .withStartupTimeout(Duration.of(60, SECONDS));
        this.configuration = new SolrContainerConfiguration();
    }

    public SELF withZookeeper(boolean zookeeper) {
        configuration.setZookeeper(zookeeper);
        return self();
    }

    public SELF withCollection(String collection) {
        if (StringUtils.isEmpty(collection)) {
            throw new IllegalArgumentException();
        }
        configuration.setCollectionName(collection);
        return self();
    }

    public SELF withConfiguration(String name, URL solrConfig) {
        if (StringUtils.isEmpty(name) || solrConfig == null) {
            throw new IllegalArgumentException();
        }
        configuration.setConfigurationName(name);
        configuration.setSolrConfiguration(solrConfig);
        return self();
    }

    public SELF withSchema(URL schema) {
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
            throw new ConfigurationException("Solr needs to have a configuration is you want to use a schema");
        }
        // Generate Command Builder
        String command = "solr -f";
        // Add Default Ports
        this.addExposedPort(SOLR_PORT);

        // Configure Zookeeper
        if (configuration.isZookeeper()) {
            this.addExposedPort(ZOOKEEPER_PORT);
            command = "-DzkRun -h localhost";
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
            execInContainer("solr", "create_core", "-c", configuration.getCollectionName());
            return;
        }

        if (StringUtils.isNotEmpty(configuration.getConfigurationName())) {
            SolrClientUtils.uploadConfiguration(getSolrPort(),
                configuration.getConfigurationName(),
                configuration.getSolrConfiguration(),
                configuration.getSolrSchema());
        }

        SolrClientUtils.createCollection(getSolrPort(), configuration.getCollectionName(), configuration.getConfigurationName());
    }
}

