package org.testcontainers.dockerclient.auth;

import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import org.junit.Test;

/**
 * TODO: Javadocs
 */
public class AuthDelegatingDockerClientConfigTest {

    @Test
    public void simpleTest() {
        final DefaultDockerClientConfig defaultConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        final AuthDelegatingDockerClientConfig config = new AuthDelegatingDockerClientConfig(defaultConfig);
        final AuthConfig authConfig = config.effectiveAuthConfig("richnorth/dummy-private-repo");
    }
}
