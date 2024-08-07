package org.testcontainers.utility;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class TestcontainersConfigurationTest {

    private Properties userProperties;

    private Properties classpathProperties;

    private Map<String, String> environment;

    @Before
    public void setUp() {
        userProperties = new Properties();
        classpathProperties = new Properties();
        environment = new HashMap<>();
    }

    @Test
    public void shouldSubstituteImageNamesFromClasspathProperties() {
        classpathProperties.setProperty("ryuk.container.image", "foo:version");
        assertThat(newConfig().getConfiguredSubstituteImage(DockerImageName.parse("testcontainers/ryuk:any")))
            .as("an image name can be pulled from classpath properties")
            .isEqualTo(DockerImageName.parse("foo:version"));
    }

    @Test
    public void shouldSubstituteImageNamesFromUserProperties() {
        userProperties.setProperty("ryuk.container.image", "foo:version");
        assertThat(newConfig().getConfiguredSubstituteImage(DockerImageName.parse("testcontainers/ryuk:any")))
            .as("an image name can be pulled from user properties")
            .isEqualTo(DockerImageName.parse("foo:version"));
    }

    @Test
    public void shouldSubstituteImageNamesFromEnvironmentVariables() {
        environment.put("TESTCONTAINERS_RYUK_CONTAINER_IMAGE", "foo:version");
        assertThat(newConfig().getConfiguredSubstituteImage(DockerImageName.parse("testcontainers/ryuk:any")))
            .as("an image name can be pulled from an environment variable")
            .isEqualTo(DockerImageName.parse("foo:version"));
    }

    @Test
    public void shouldApplySettingsInOrder() {
        assertThat(newConfig().getEnvVarOrProperty("key", "default"))
            .as("precedence order for multiple sources of the same value is correct")
            .isEqualTo("default");

        classpathProperties.setProperty("key", "foo");

        assertThat(newConfig().getEnvVarOrProperty("key", "default"))
            .as("precedence order for multiple sources of the same value is correct")
            .isEqualTo("foo");

        userProperties.setProperty("key", "bar");

        assertThat(newConfig().getEnvVarOrProperty("key", "default"))
            .as("precedence order for multiple sources of the same value is correct")
            .isEqualTo("bar");

        environment.put("TESTCONTAINERS_KEY", "baz");

        assertThat(newConfig().getEnvVarOrProperty("key", "default"))
            .as("precedence order for multiple sources of the same value is correct")
            .isEqualTo("baz");
    }

    @Test
    public void shouldNotReadChecksFromClasspathProperties() {
        assertThat(newConfig().isDisableChecks()).as("checks enabled by default").isFalse();

        classpathProperties.setProperty("checks.disable", "true");
        assertThat(newConfig().isDisableChecks()).as("checks are not affected by classpath properties").isFalse();
    }

    @Test
    public void shouldReadChecksFromUserProperties() {
        assertThat(newConfig().isDisableChecks()).as("checks enabled by default").isFalse();

        userProperties.setProperty("checks.disable", "true");
        assertThat(newConfig().isDisableChecks()).as("checks disabled via user properties").isTrue();
    }

    @Test
    public void shouldReadChecksFromEnvironment() {
        assertThat(newConfig().isDisableChecks()).as("checks enabled by default").isFalse();

        userProperties.remove("checks.disable");
        environment.put("TESTCONTAINERS_CHECKS_DISABLE", "true");
        assertThat(newConfig().isDisableChecks()).as("checks disabled via env var").isTrue();
    }

    @Test
    public void shouldReadDockerSettingsFromEnvironmentWithoutTestcontainersPrefix() {
        userProperties.remove("docker.foo");
        environment.put("DOCKER_FOO", "some value");
        assertThat(newConfig().getEnvVarOrUserProperty("docker.foo", "default"))
            .as("reads unprefixed env vars for docker. settings")
            .isEqualTo("some value");
    }

    @Test
    public void shouldNotReadDockerSettingsFromEnvironmentWithTestcontainersPrefix() {
        userProperties.remove("docker.foo");
        environment.put("TESTCONTAINERS_DOCKER_FOO", "some value");
        assertThat(newConfig().getEnvVarOrUserProperty("docker.foo", "default"))
            .as("reads unprefixed env vars for docker. settings")
            .isEqualTo("default");
    }

    @Test
    public void shouldReadDockerSettingsFromUserProperties() {
        environment.remove("DOCKER_FOO");
        userProperties.put("docker.foo", "some value");
        assertThat(newConfig().getEnvVarOrUserProperty("docker.foo", "default"))
            .as("reads unprefixed user properties for docker. settings")
            .isEqualTo("some value");
    }

    @Test
    public void shouldNotReadSettingIfCorrespondingEnvironmentVarIsEmptyString() {
        environment.put("DOCKER_FOO", "");
        assertThat(newConfig().getEnvVarOrUserProperty("docker.foo", "default"))
            .as("reads unprefixed env vars for docker. settings")
            .isEqualTo("default");
    }

    @Test
    public void shouldNotReadDockerClientStrategyFromClasspathProperties() {
        String currentValue = newConfig().getDockerClientStrategyClassName();

        classpathProperties.setProperty("docker.client.strategy", UUID.randomUUID().toString());
        assertThat(newConfig().getDockerClientStrategyClassName())
            .as("Docker client strategy is not affected by classpath properties")
            .isEqualTo(currentValue);
    }

    @Test
    public void shouldReadDockerClientStrategyFromUserProperties() {
        userProperties.setProperty("docker.client.strategy", "foo");
        assertThat(newConfig().getDockerClientStrategyClassName())
            .as("Docker client strategy is changed by user property")
            .isEqualTo("foo");
    }

    @Test
    public void shouldReadDockerClientStrategyFromEnvironment() {
        userProperties.remove("docker.client.strategy");
        environment.put("TESTCONTAINERS_DOCKER_CLIENT_STRATEGY", "foo");
        assertThat(newConfig().getDockerClientStrategyClassName())
            .as("Docker client strategy is changed by env var")
            .isEqualTo("foo");
    }

    @Test
    public void shouldNotUseImplicitDockerClientStrategyWhenDockerHostAndStrategyAreBothSet() {
        userProperties.put("docker.client.strategy", "foo");
        userProperties.put("docker.host", "tcp://1.2.3.4:5678");
        assertThat(newConfig().getDockerClientStrategyClassName())
            .as("Docker client strategy is can be explicitly set")
            .isEqualTo("foo");

        userProperties.remove("docker.client.strategy");

        environment.put("TESTCONTAINERS_DOCKER_CLIENT_STRATEGY", "bar");
        userProperties.put("docker.client.strategy", "foo");
        assertThat(newConfig().getDockerClientStrategyClassName())
            .as("Docker client strategy is can be explicitly set")
            .isEqualTo("bar");

        environment.put("TESTCONTAINERS_DOCKER_CLIENT_STRATEGY", "bar");
        userProperties.remove("docker.client.strategy");
        assertThat(newConfig().getDockerClientStrategyClassName())
            .as("Docker client strategy is can be explicitly set")
            .isEqualTo("bar");

        environment.remove("TESTCONTAINERS_DOCKER_CLIENT_STRATEGY");
        userProperties.put("docker.client.strategy", "foo");
        assertThat(newConfig().getDockerClientStrategyClassName())
            .as("Docker client strategy is can be explicitly set")
            .isEqualTo("foo");
    }

    @Test
    public void shouldNotReadReuseFromClasspathProperties() {
        assertThat(newConfig().environmentSupportsReuse()).as("no reuse by default").isFalse();

        classpathProperties.setProperty("testcontainers.reuse.enable", "true");
        assertThat(newConfig().environmentSupportsReuse())
            .as("reuse is not affected by classpath properties")
            .isFalse();
    }

    @Test
    public void shouldReadReuseFromUserProperties() {
        assertThat(newConfig().environmentSupportsReuse()).as("no reuse by default").isFalse();

        userProperties.setProperty("testcontainers.reuse.enable", "true");
        assertThat(newConfig().environmentSupportsReuse()).as("reuse enabled via user property").isTrue();
    }

    @Test
    public void shouldReadReuseFromEnvironment() {
        assertThat(newConfig().environmentSupportsReuse()).as("no reuse by default").isFalse();

        userProperties.remove("testcontainers.reuse.enable");
        environment.put("TESTCONTAINERS_REUSE_ENABLE", "true");
        assertThat(newConfig().environmentSupportsReuse()).as("reuse enabled via env var").isTrue();
    }

    @Test
    public void shouldTrimImageNames() {
        userProperties.setProperty("ryuk.container.image", " testcontainers/ryuk:0.3.2 ");
        assertThat(newConfig().getRyukImage())
            .as("trailing whitespace was not removed from image name property")
            .isEqualTo("testcontainers/ryuk:0.3.2");
    }

    @Test
    public void shouldNotReadRyukShutdownHookClasspathProperties() {
        assertThat(newConfig().isRyukShutdownHookEnabled()).as("Ryuk shutdown hook disabled by default").isFalse();

        classpathProperties.setProperty("ryuk.container.shutdownhook", "true");
        assertThat(newConfig().isRyukShutdownHookEnabled())
            .as("Ryuk shutdown hook is not affected by classpath properties")
            .isFalse();
    }

    @Test
    public void shouldReadRyukShutdownHookFromUserProperties() {
        assertThat(newConfig().isRyukShutdownHookEnabled()).as("Ryuk shutdown hook disabled by default").isFalse();

        userProperties.setProperty("ryuk.container.shutdownhook", "true");
        assertThat(newConfig().isRyukShutdownHookEnabled())
            .as("Ryuk shutdown hook enabled via user properties")
            .isTrue();
    }

    @Test
    public void shouldReadRyukShutdownHookFromEnvironment() {
        assertThat(newConfig().isRyukShutdownHookEnabled()).as("Ryuk shutdown hook disabled by default").isFalse();

        userProperties.remove("ryuk.container.shutdownhook");
        environment.put("TESTCONTAINERS_RYUK_CONTAINER_SHUTDOWNHOOK", "true");
        assertThat(newConfig().isRyukShutdownHookEnabled()).as("Ryuk shutdown hook enabled via env var").isTrue();
    }

    private TestcontainersConfiguration newConfig() {
        return new TestcontainersConfiguration(userProperties, classpathProperties, environment);
    }
}
