package org.testcontainers.containers;

import com.hazelcast.core.HazelcastInstance;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class HazelcastContainerTest {

    private static final Logger logger = LoggerFactory.getLogger(HazelcastContainerTest.class);

    @ClassRule
    public static HazelcastContainer<?> hazelcastContainer = new HazelcastContainer<>()
        .withHazelcastConfigFile("hazelcast.yml")
        .withHazelcastLoggingLevel(HazelcastContainer.LogLevel.INFO)
        .withHazelcastJavaOpts("-Xms128M -Xmx256M")
        .withHazelcastPrometheusPort(8080)
        .withHazelcastJMXPort(9999)
        .withLogConsumer(new Slf4jLogConsumer(logger));

    @Test
    public void shouldReturnURL() {
        String actual = hazelcastContainer.getUrl();

        assertThat(actual).isNotEmpty();
    }

    @Test
    public void shouldReturnLivenessPorts() {
        Set<Integer> actual = hazelcastContainer.getLivenessCheckPortNumbers();

        assertThat(actual).isNotEmpty();
    }

    @Test
    public void shouldReachRunningState() {
        boolean actual = hazelcastContainer.isRunning();

        assertThat(actual).isTrue();
    }

    @Test
    public void shouldReturnAValidClient() {
        HazelcastInstance hazelcastClient = hazelcastContainer.getHazelcastClient();
        hazelcastClient.getMap("foo").put("a", "b");

        assertThat(hazelcastClient.getMap("foo")).containsKey("a");
    }
}
