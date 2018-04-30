package org.testcontainers.utility;

import com.github.dockerjava.api.model.AuthConfig;
import com.google.common.io.Resources;
import org.apache.commons.lang.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertNull;

public class RegistryAuthLocatorTest {

    @BeforeClass
    public static void nonWindowsTest() throws Exception {
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);
    }

    @Test
    public void lookupAuthConfigWithoutCredentials() throws URISyntaxException {
        final RegistryAuthLocator authLocator = createTestAuthLocator("config-with-store.json");

        final AuthConfig authConfig = authLocator.lookupAuthConfig(new DockerImageName("unauthenticated.registry.org/org/repo"));

        assertEquals("Default docker registry URL is set on auth config", "https://index.docker.io/v1/", authConfig.getRegistryAddress());
        assertNull("No username is set", authConfig.getUsername());
        assertNull("No password is set", authConfig.getPassword());
    }

    @Test
    public void lookupAuthConfigUsingStore() throws URISyntaxException {
        final RegistryAuthLocator authLocator = createTestAuthLocator("config-with-store.json");

        final AuthConfig authConfig = authLocator.lookupAuthConfig(new DockerImageName("registry.example.com/org/repo"));

        assertEquals("Correct server URL is obtained from a credential store", "url", authConfig.getRegistryAddress());
        assertEquals("Correct username is obtained from a credential store", "username", authConfig.getUsername());
        assertEquals("Correct secret is obtained from a credential store", "secret", authConfig.getPassword());
    }

    @Test
    public void lookupAuthConfigUsingHelper() throws URISyntaxException {
        final RegistryAuthLocator authLocator = createTestAuthLocator("config-with-helper.json");

        final AuthConfig authConfig = authLocator.lookupAuthConfig(new DockerImageName("registry.example.com/org/repo"));

        assertEquals("Correct server URL is obtained from a credential store", "url", authConfig.getRegistryAddress());
        assertEquals("Correct username is obtained from a credential store", "username", authConfig.getUsername());
        assertEquals("Correct secret is obtained from a credential store", "secret", authConfig.getPassword());
    }

    @NotNull
    private RegistryAuthLocator createTestAuthLocator(String configName) throws URISyntaxException {
        final File configFile = Paths.get(Resources.getResource("auth-config/" + configName).toURI()).toFile();
        return new RegistryAuthLocator(new AuthConfig(), configFile, configFile.getParentFile().getAbsolutePath() + "/");
    }

}
