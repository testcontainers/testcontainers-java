package org.testcontainers.utility;

import com.github.dockerjava.api.model.AuthConfig;
import com.google.common.io.Resources;
import org.apache.commons.lang.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.rnorth.visibleassertions.VisibleAssertions.*;

public class RegistryAuthLocatorTest {
    @Test
    public void lookupAuthConfigWithoutCredentials() throws URISyntaxException {
        final RegistryAuthLocator authLocator = createTestAuthLocator("config-empty.json");

        final AuthConfig authConfig = authLocator.lookupAuthConfig(DockerImageName.parse("unauthenticated.registry.org/org/repo"), new AuthConfig());

        assertEquals("Default docker registry URL is set on auth config", "https://index.docker.io/v1/", authConfig.getRegistryAddress());
        assertNull("No username is set", authConfig.getUsername());
        assertNull("No password is set", authConfig.getPassword());
    }

    @Test
    public void lookupAuthConfigWithBasicAuthCredentials() throws URISyntaxException {
        final RegistryAuthLocator authLocator = createTestAuthLocator("config-basic-auth.json");

        final AuthConfig authConfig = authLocator.lookupAuthConfig(DockerImageName.parse("registry.example.com/org/repo"), new AuthConfig());

        assertEquals("Default docker registry URL is set on auth config", "https://registry.example.com", authConfig.getRegistryAddress());
        assertEquals("Username is set", "user", authConfig.getUsername());
        assertEquals("Password is set", "pass", authConfig.getPassword());
    }

    @Test
    public void lookupAuthConfigWithJsonKeyCredentials() throws URISyntaxException {
        final RegistryAuthLocator authLocator = createTestAuthLocator("config-with-json-key.json");

        final AuthConfig authConfig = authLocator.lookupAuthConfig(DockerImageName.parse("registry.example.com/org/repo"), new AuthConfig());

        assertEquals("Default docker registry URL is set on auth config", "https://registry.example.com", authConfig.getRegistryAddress());
        assertEquals("Username is set", "_json_key", authConfig.getUsername());
        assertNotNull("Password is set", authConfig.getPassword());
    }

    @Test
    public void lookupAuthConfigUsingStore() throws URISyntaxException {
        final RegistryAuthLocator authLocator = createTestAuthLocator("config-with-store.json");

        final AuthConfig authConfig = authLocator.lookupAuthConfig(DockerImageName.parse("registry.example.com/org/repo"), new AuthConfig());

        assertEquals("Correct server URL is obtained from a credential store", "url", authConfig.getRegistryAddress());
        assertEquals("Correct username is obtained from a credential store", "username", authConfig.getUsername());
        assertEquals("Correct secret is obtained from a credential store", "secret", authConfig.getPassword());
    }

    @Test
    public void lookupAuthConfigUsingHelper() throws URISyntaxException {
        final RegistryAuthLocator authLocator = createTestAuthLocator("config-with-helper.json");

        final AuthConfig authConfig = authLocator.lookupAuthConfig(DockerImageName.parse("registry.example.com/org/repo"), new AuthConfig());

        assertEquals("Correct server URL is obtained from a credential store", "url", authConfig.getRegistryAddress());
        assertEquals("Correct username is obtained from a credential store", "username", authConfig.getUsername());
        assertEquals("Correct secret is obtained from a credential store", "secret", authConfig.getPassword());
    }

    @Test
    public void lookupAuthConfigUsingHelperWithToken() throws URISyntaxException {
        final RegistryAuthLocator authLocator = createTestAuthLocator("config-with-helper-using-token.json");

        final AuthConfig authConfig = authLocator.lookupAuthConfig(DockerImageName.parse("registrytoken.example.com/org/repo"), new AuthConfig());

        assertEquals("Correct identitytoken is obtained from a credential store", "secret", authConfig.getIdentitytoken());
    }

    @Test
    public void lookupUsingHelperEmptyAuth() throws URISyntaxException {
        final RegistryAuthLocator authLocator = createTestAuthLocator("config-empty-auth-with-helper.json");

        final AuthConfig authConfig = authLocator.lookupAuthConfig(DockerImageName.parse("registry.example.com/org/repo"), new AuthConfig());

        assertEquals("Correct server URL is obtained from a credential store", "url", authConfig.getRegistryAddress());
        assertEquals("Correct username is obtained from a credential store", "username", authConfig.getUsername());
        assertEquals("Correct secret is obtained from a credential store", "secret", authConfig.getPassword());
    }

    @Test
    public void lookupNonEmptyAuthWithHelper() throws URISyntaxException {
        final RegistryAuthLocator authLocator = createTestAuthLocator("config-existing-auth-with-helper.json");

        final AuthConfig authConfig = authLocator.lookupAuthConfig(DockerImageName.parse("registry.example.com/org/repo"), new AuthConfig());

        assertEquals("Correct server URL is obtained from a credential helper", "url", authConfig.getRegistryAddress());
        assertEquals("Correct username is obtained from a credential helper", "username", authConfig.getUsername());
        assertEquals("Correct password is obtained from a credential helper", "secret", authConfig.getPassword());
    }

    @Test
    public void lookupAuthConfigWithCredentialsNotFound() throws URISyntaxException {
        Map<String, String> notFoundMessagesReference = new HashMap<>();
        final RegistryAuthLocator authLocator = createTestAuthLocator("config-with-store.json", notFoundMessagesReference);

        DockerImageName dockerImageName = DockerImageName.parse("registry2.example.com/org/repo");
        final AuthConfig authConfig = authLocator.lookupAuthConfig(dockerImageName, new AuthConfig());

        assertNull("No username should have been obtained from a credential store", authConfig.getUsername());
        assertNull("No secret should have been obtained from a credential store", authConfig.getPassword());
        assertEquals("Should have one 'credentials not found' message discovered", 1, notFoundMessagesReference.size());

        String discoveredMessage = notFoundMessagesReference.values().iterator().next();

        assertEquals(
            "Not correct message discovered",
            "Fake credentials not found on credentials store 'https://not.a.real.registry/url'",
            discoveredMessage);
    }

    @Test
    public void lookupAuthConfigWithCredStoreEmpty() throws URISyntaxException {
        final RegistryAuthLocator authLocator = createTestAuthLocator("config-with-store-empty.json");

        DockerImageName dockerImageName = DockerImageName.parse("registry2.example.com/org/repo");
        final AuthConfig authConfig = authLocator.lookupAuthConfig(dockerImageName, new AuthConfig());

        assertNull("CredStore field will be ignored, because value is blank", authConfig.getAuth());
    }

    @NotNull
    private RegistryAuthLocator createTestAuthLocator(String configName) throws URISyntaxException {
        return createTestAuthLocator(configName, new HashMap<>());
    }

    @NotNull
    private RegistryAuthLocator createTestAuthLocator(String configName, Map<String, String> notFoundMessagesReference) throws URISyntaxException {
        final File configFile = new File(Resources.getResource("auth-config/" + configName).toURI());

        String commandPathPrefix = configFile.getParentFile().getAbsolutePath() + "/";
        String commandExtension = "";

        if (SystemUtils.IS_OS_WINDOWS) {
            commandPathPrefix += "win/";

            // need to provide executable extension otherwise won't run it
            // with real docker wincredential exe there is no problem
            commandExtension = ".bat";
        }

        return new RegistryAuthLocator(configFile, commandPathPrefix, commandExtension, notFoundMessagesReference);
    }
}
