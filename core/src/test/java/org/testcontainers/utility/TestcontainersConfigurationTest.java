package org.testcontainers.utility;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertFalse;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

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
        assertEquals(
            "an image name can be pulled from classpath properties",
            DockerImageName.parse("foo:version"),
            newConfig().getConfiguredSubstituteImage(DockerImageName.parse("testcontainers/ryuk:any"))
        );
    }

    @Test
    public void shouldSubstituteImageNamesFromUserProperties() {
        userProperties.setProperty("ryuk.container.image", "foo:version");
        assertEquals(
            "an image name can be pulled from user properties",
            DockerImageName.parse("foo:version"),
            newConfig().getConfiguredSubstituteImage(DockerImageName.parse("testcontainers/ryuk:any"))
        );
    }

    @Test
    public void shouldSubstituteImageNamesFromEnvironmentVariables() {
        environment.put("TESTCONTAINERS_RYUK_CONTAINER_IMAGE", "foo:version");
        assertEquals(
            "an image name can be pulled from an environment variable",
            DockerImageName.parse("foo:version"),
            newConfig().getConfiguredSubstituteImage(DockerImageName.parse("testcontainers/ryuk:any"))
        );
    }

    @Test
    public void shouldApplySettingsInOrder() {
        assertEquals(
            "precedence order for multiple sources of the same value is correct",
            "default",
            newConfig().getEnvVarOrProperty("key", "default")
        );

        classpathProperties.setProperty("key", "foo");

        assertEquals(
            "precedence order for multiple sources of the same value is correct",
            "foo",
            newConfig().getEnvVarOrProperty("key", "default")
        );

        userProperties.setProperty("key", "bar");

        assertEquals(
            "precedence order for multiple sources of the same value is correct",
            "bar",
            newConfig().getEnvVarOrProperty("key", "default")
        );

        environment.put("TESTCONTAINERS_KEY", "baz");

        assertEquals(
            "precedence order for multiple sources of the same value is correct",
            "baz",
            newConfig().getEnvVarOrProperty("key", "default")
        );
    }

    @Test
    public void shouldNotReadChecksFromClasspathProperties() {
        assertFalse("checks enabled by default", newConfig().isDisableChecks());

        classpathProperties.setProperty("checks.disable", "true");
        assertFalse("checks are not affected by classpath properties", newConfig().isDisableChecks());
    }

    @Test
    public void shouldReadChecksFromUserProperties() {
        assertFalse("checks enabled by default", newConfig().isDisableChecks());

        userProperties.setProperty("checks.disable", "true");
        assertTrue("checks disabled via user properties", newConfig().isDisableChecks());
    }

    @Test
    public void shouldReadChecksFromEnvironment() {
        assertFalse("checks enabled by default", newConfig().isDisableChecks());

        userProperties.remove("checks.disable");
        environment.put("TESTCONTAINERS_CHECKS_DISABLE", "true");
        assertTrue("checks disabled via env var", newConfig().isDisableChecks());
    }

    @Test
    public void shouldReadDockerSettingsFromEnvironmentWithoutTestcontainersPrefix() {
        userProperties.remove("docker.foo");
        environment.put("DOCKER_FOO", "some value");
        assertEquals("reads unprefixed env vars for docker. settings", "some value", newConfig().getEnvVarOrUserProperty("docker.foo", "default"));
    }

    @Test
    public void shouldNotReadDockerSettingsFromEnvironmentWithTestcontainersPrefix() {
        userProperties.remove("docker.foo");
        environment.put("TESTCONTAINERS_DOCKER_FOO", "some value");
        assertEquals("reads unprefixed env vars for docker. settings", "default", newConfig().getEnvVarOrUserProperty("docker.foo", "default"));
    }

    @Test
    public void shouldReadDockerSettingsFromUserProperties() {
        environment.remove("DOCKER_FOO");
        userProperties.put("docker.foo", "some value");
        assertEquals("reads unprefixed user properties for docker. settings", "some value", newConfig().getEnvVarOrUserProperty("docker.foo", "default"));
    }

    @Test
    public void shouldNotReadDockerClientStrategyFromClasspathProperties() {
        String currentValue = newConfig().getDockerClientStrategyClassName();

        classpathProperties.setProperty("docker.client.strategy", UUID.randomUUID().toString());
        assertEquals("Docker client strategy is not affected by classpath properties", currentValue, newConfig().getDockerClientStrategyClassName());
    }

    @Test
    public void shouldReadDockerClientStrategyFromUserProperties() {
        userProperties.setProperty("docker.client.strategy", "foo");
        assertEquals("Docker client strategy is changed by user property", "foo", newConfig().getDockerClientStrategyClassName());
    }

    @Test
    public void shouldReadDockerClientStrategyFromEnvironment() {
        userProperties.remove("docker.client.strategy");
        environment.put("TESTCONTAINERS_DOCKER_CLIENT_STRATEGY", "foo");
        assertEquals("Docker client strategy is changed by env var", "foo", newConfig().getDockerClientStrategyClassName());
    }

    @Test
    public void shouldNotUseImplicitDockerClientStrategyWhenDockerHostAndStrategyAreBothSet() {
        userProperties.put("docker.client.strategy", "foo");
        userProperties.put("docker.host", "tcp://1.2.3.4:5678");
        assertEquals("Docker client strategy is can be explicitly set", "foo", newConfig().getDockerClientStrategyClassName());

        userProperties.remove("docker.client.strategy");

        environment.put("TESTCONTAINERS_DOCKER_CLIENT_STRATEGY", "bar");
        userProperties.put("docker.client.strategy", "foo");
        assertEquals("Docker client strategy is can be explicitly set", "bar", newConfig().getDockerClientStrategyClassName());

        environment.put("TESTCONTAINERS_DOCKER_CLIENT_STRATEGY", "bar");
        userProperties.remove("docker.client.strategy");
        assertEquals("Docker client strategy is can be explicitly set", "bar", newConfig().getDockerClientStrategyClassName());

        environment.remove("TESTCONTAINERS_DOCKER_CLIENT_STRATEGY");
        userProperties.put("docker.client.strategy", "foo");
        assertEquals("Docker client strategy is can be explicitly set", "foo", newConfig().getDockerClientStrategyClassName());
    }

    @Test
    public void shouldNotReadReuseFromClasspathProperties() {
        assertFalse("no reuse by default", newConfig().environmentSupportsReuse());

        classpathProperties.setProperty("testcontainers.reuse.enable", "true");
        assertFalse("reuse is not affected by classpath properties", newConfig().environmentSupportsReuse());
    }

    @Test
    public void shouldReadReuseFromUserProperties() {
        assertFalse("no reuse by default", newConfig().environmentSupportsReuse());

        userProperties.setProperty("testcontainers.reuse.enable", "true");
        assertTrue("reuse enabled via user property", newConfig().environmentSupportsReuse());
    }
    @Test
    public void shouldReadReuseFromEnvironment() {
        assertFalse("no reuse by default", newConfig().environmentSupportsReuse());

        userProperties.remove("testcontainers.reuse.enable");
        environment.put("TESTCONTAINERS_REUSE_ENABLE", "true");
        assertTrue("reuse enabled via env var", newConfig().environmentSupportsReuse());
    }

    @Test
    public void shouldTrimImageNames() {
        userProperties.setProperty("ryuk.container.image", " testcontainers/ryuk:0.3.2 ");
        assertEquals("trailing whitespace was not removed from image name property", "testcontainers/ryuk:0.3.2",newConfig().getRyukImage());
    }

    private TestcontainersConfiguration newConfig() {
        return new TestcontainersConfiguration(userProperties, classpathProperties, environment);
    }
}
