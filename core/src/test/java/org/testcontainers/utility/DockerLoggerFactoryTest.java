package org.testcontainers.utility;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DockerLoggerFactoryTest {

    private static final Logger LOGGER = (Logger) DockerLoggerFactory.getLogger("dockerImageName");

    @Test
    public void debugIsNotSwallowedForContainerLogs() {
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        LOGGER.addAppender(listAppender);

        LOGGER.debug("some text");

        assertThat(listAppender.list).withFailMessage("Log message has been swallowed").hasSize(1);

        ILoggingEvent event = listAppender.list.get(0);

        assertThat(event.getFormattedMessage()).isEqualTo("some text");
        assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
        assertThat(event.getLoggerName()).startsWith("tc");
    }
}
