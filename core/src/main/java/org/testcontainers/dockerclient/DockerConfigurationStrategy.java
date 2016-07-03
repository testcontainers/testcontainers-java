package org.testcontainers.dockerclient;

import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.core.DockerClientConfig;
import com.google.common.base.Throwables;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Mechanism to find a viable Docker client configuration according to the host system environment.
 */
public interface DockerConfigurationStrategy {

    /**
     * @return a usable, tested, Docker client configuration for the host system environment
     * @throws InvalidConfigurationException if this strategy fails
     * @param cmdExecFactory
     */
    DockerClientConfig provideConfiguration(DockerCmdExecFactory cmdExecFactory) throws InvalidConfigurationException;

    /**
     * @return a short textual description of the strategy
     */
    String getDescription();

    Logger LOGGER = LoggerFactory.getLogger(DockerConfigurationStrategy.class);

    /**
     * Determine the right DockerClientConfig to use for building clients by trial-and-error.
     *
     * @return a working DockerClientConfig, as determined by successful execution of a ping command
     */
    static DockerClientConfig getFirstValidConfig(List<DockerConfigurationStrategy> strategies, DockerCmdExecFactory cmdExecFactory) {
        List<String> configurationFailures = new ArrayList<>();

        for (DockerConfigurationStrategy strategy : strategies) {
            try {
                LOGGER.info("Looking for Docker environment. Trying {}", strategy.getDescription());
                return strategy.provideConfiguration(cmdExecFactory);
            } catch (Exception | ExceptionInInitializerError e) {
                @Nullable String throwableMessage = e.getMessage();
                Throwable rootCause = Throwables.getRootCause(e);
                @Nullable String rootCauseMessage = rootCause.getMessage();

                String failureDescription;
                if (throwableMessage != null && throwableMessage.equals(rootCauseMessage)) {
                    failureDescription = String.format("%s: failed with exception %s (%s)",
                            strategy.getClass().getSimpleName(),
                            e.getClass().getSimpleName(),
                            throwableMessage);
                } else {
                    failureDescription = String.format("%s: failed with exception %s (%s). Root cause %s (%s)",
                            strategy.getClass().getSimpleName(),
                            e.getClass().getSimpleName(),
                            throwableMessage,
                            rootCause.getClass().getSimpleName(),
                            rootCauseMessage
                            );
                }
                configurationFailures.add(failureDescription);

                LOGGER.debug(failureDescription);
            }
        }

        LOGGER.error("Could not find a valid Docker environment. Please check configuration. Attempted configurations were:");
        for (String failureMessage : configurationFailures) {
            LOGGER.error("    " + failureMessage);
        }
        LOGGER.error("As no valid configuration was found, execution cannot continue");

        throw new IllegalStateException("Could not find a valid Docker environment. Please see logs and check configuration");
    }


    class InvalidConfigurationException extends RuntimeException {

        public InvalidConfigurationException(String s) {
            super(s);
        }

        public InvalidConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
