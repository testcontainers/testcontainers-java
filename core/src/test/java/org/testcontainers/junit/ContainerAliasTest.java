package org.testcontainers.junit;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.TestImages;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContainerAliasTest {

    private final TestLogger testLogger = new TestLogger();

    public GenericContainer container1 = new GenericContainer(TestImages.ALPINE_IMAGE)
        .withContainerAlias("potato")
        .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
        .withCommand("ls", "-al");

    public GenericContainer container2 = new GenericContainer(TestImages.ALPINE_IMAGE)
        .withContainerAlias("monkey")
        .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
        .withCommand("non-existing-command");

    @BeforeEach
    void setUp() {
        testLogger.startCapturing();
    }

    @AfterEach
    void tearDown() {
        testLogger.stopCapturing();
    }

    @Test
    void checkOutput() {
        assertThatNoException()
            .isThrownBy(() -> {
                container1.start();
            });

        assertThatThrownBy(() -> {
                container2.start();
            })
            .isInstanceOf(ContainerLaunchException.class)
            .hasMessage("Container startup failed for image alpine:3.17 (containerAlias='monkey')");

        List<String> container1PotatoLogs = testLogger
            .getLogs()
            .stream()
            .filter(it -> "tc.alpine:3.17--potato".equals(it.getLoggerName()))
            .map(ILoggingEvent::getFormattedMessage)
            .toList();
        assertThat(container1PotatoLogs)
            .anyMatch(it -> it.equals("Creating container for image: alpine:3.17"))
            .anyMatch(it -> it.startsWith("Container alpine:3.17 is starting: "))
            .anyMatch(it -> it.startsWith("Container alpine:3.17 started in P"));

        List<String> container2MonkeyLogs = testLogger
            .getLogs()
            .stream()
            .filter(it -> "tc.alpine:3.17--monkey".equals(it.getLoggerName()))
            .map(ILoggingEvent::getFormattedMessage)
            .toList();
        assertThat(container2MonkeyLogs)
            .anyMatch(it -> it.equals("Creating container for image: alpine:3.17"))
            .anyMatch(it -> it.startsWith("Container alpine:3.17 is starting: "))
            .anyMatch(it -> it.equals("Could not start container"))
            .anyMatch(it -> it.equals("There are no stdout/stderr logs available for the failed container"));
    }
}

class TestLogger {

    private final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

    public void startCapturing() {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        listAppender.start();
        rootLogger.addAppender(listAppender);
    }

    public void stopCapturing() {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.detachAppender(listAppender);
        listAppender.stop();
    }

    public List<ILoggingEvent> getLogs() {
        return listAppender.list;
    }
}
