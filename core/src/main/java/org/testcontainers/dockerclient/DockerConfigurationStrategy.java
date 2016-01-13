package org.testcontainers.dockerclient;

import com.github.dockerjava.core.DockerClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rnorth on 13/01/2016.
 */
public interface DockerConfigurationStrategy {

    DockerClientConfig provideConfiguration() throws InvalidConfigurationException;

    String getDescription();

    Logger LOGGER = LoggerFactory.getLogger(DockerConfigurationStrategy.class);

    /**
     * Determine the right DockerClientConfig to use for building clients by trial-and-error.
     *
     * @return a working DockerClientConfig, as determined by successful execution of a ping command
     */
    static DockerClientConfig getFirstValidConfig(List<DockerConfigurationStrategy> strategies) {
        Map<DockerConfigurationStrategy, Exception> configurationFailures = new LinkedHashMap<>();

        for (DockerConfigurationStrategy strategy : strategies) {
            try {
                LOGGER.info("Looking for Docker environment. Trying {}", strategy.getDescription());
                return strategy.provideConfiguration();
            } catch (Exception e) {
                configurationFailures.put(strategy, e);
            }
        }

        LOGGER.error("Could not find a valid Docker environment. Please check configuration. Attempted configurations were:");
        for (Map.Entry<DockerConfigurationStrategy, Exception> entry : configurationFailures.entrySet()) {
            LOGGER.error("    {}: failed with exception message: {}", entry.getKey().getDescription(), entry.getValue().getMessage());
        }
        LOGGER.error("As no valid configuration was found, execution cannot continue");

        throw new IllegalStateException("Could not find a valid Docker environment. Please see logs and check configuration");
    }


    class InvalidConfigurationException extends RuntimeException {

        public InvalidConfigurationException(String s) {
            super(s);
        }
    }
}
