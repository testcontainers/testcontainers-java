package org.testcontainers.utility;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.assertj.core.api.Condition;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DockerLoggerFactoryTest {

    private static final Logger LOGGER = (Logger) DockerLoggerFactory.getLogger("dockerImageName");

    @Test
    public void debugIsNotSwallowedForContainerLogs() {
        // Arrange
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        LOGGER.addAppender(listAppender);

        // Act
        LOGGER.debug("some text");

        // Assert
        assertThat(listAppender.list)
            .withFailMessage("Log message has been swallowed")
            .hasSize(1);

        ILoggingEvent event = listAppender.list.get(0);

        assertThat(event.getFormattedMessage()).isEqualTo("some text");
        assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
        assertThat(event.getLoggerName()).startsWith("docker");
    }
}
