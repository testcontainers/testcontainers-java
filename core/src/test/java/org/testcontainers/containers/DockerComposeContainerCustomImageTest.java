package org.testcontainers.containers;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DockerComposeContainerCustomImageTest {
    public static final String DOCKER_IMAGE = "docker/compose:debian-1.29.2";
    private static final String COMPOSE_FILE_PATH = "src/test/resources/docker-compose-imagename-parsing-v1.yml";

    private DockerComposeContainer composeContainer;
    private TestLogAppender testLogAppender;
    private Logger rootLogger;

    @Before
    public void setup() {
        testLogAppender = new TestLogAppender();
        testLogAppender.start();
        rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(testLogAppender);
        TestcontainersConfiguration.getInstance().updateUserConfig("compose.container.image", DOCKER_IMAGE);
    }

    @After
    public void tearDown() {
        rootLogger.detachAppender(testLogAppender);
        TestcontainersConfiguration.getInstance().updateUserConfig("compose.container.image", "");
        System.clearProperty("compose.container.image");
        composeContainer.stop();
    }

    @Test
    public void testWithCustomDockerImage() {
        composeContainer = new DockerComposeContainer(DockerImageName.parse(DOCKER_IMAGE),"testing", new File(COMPOSE_FILE_PATH));
        composeContainer.start();
        verifyContainerCreation();
    }

    @Test
    public void testWithCustomDockerImageAndIdentifier() {
        composeContainer = new DockerComposeContainer(DockerImageName.parse(DOCKER_IMAGE), "myidentifier", new File(COMPOSE_FILE_PATH));
        composeContainer.start();
        verifyContainerCreation();
    }

    private void verifyContainerCreation() {
        List<String> logs = testLogAppender.getLogs();

        assertThat(logs)
            .isNotNull()
            .anyMatch(line -> line.contains("Creating container for image: " + DOCKER_IMAGE));
    }

    private static class TestLogAppender extends AppenderBase<ILoggingEvent> {

        private final List<String> logs = new ArrayList<>();

        @Override
        protected void append(ILoggingEvent eventObject) {
            logs.add(eventObject.getFormattedMessage());
        }

        public List<String> getLogs() {
            return logs;
        }
    }
}
