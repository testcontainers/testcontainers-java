package org.testcontainers.dockerclient.auth;

import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.core.DockerClientConfig;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.RegistryAuthLocator;

import static org.testcontainers.utility.LogUtils.logSafe;

/**
 * TODO: Javadocs
 */
@Slf4j
public class AuthDelegatingDockerClientConfig implements DockerClientConfig {

    private final RegistryAuthLocator authLocator;

    @Delegate(excludes = DelegateExclusions.class)
    private DockerClientConfig delegate;

    public AuthDelegatingDockerClientConfig(DockerClientConfig delegate) {
        this.delegate = delegate;
        this.authLocator = new RegistryAuthLocator();
    }

    public AuthConfig effectiveAuthConfig(String imageName) {
        // allow docker-java auth config to be used as a fallback
        AuthConfig fallbackAuthConfig;
        try {
            fallbackAuthConfig = delegate.effectiveAuthConfig(imageName);
        } catch (Exception e) {
            fallbackAuthConfig = new AuthConfig();
        }

        // try and obtain more accurate auth config using our resolution
        final DockerImageName parsed = new DockerImageName(imageName);
        final AuthConfig effectiveAuthConfig = authLocator.lookupAuthConfig(parsed, fallbackAuthConfig);

        log.debug("effective auth config [{}]", logSafe(effectiveAuthConfig));
        return effectiveAuthConfig;
    }

    private interface DelegateExclusions {
        AuthConfig effectiveAuthConfig(String imageName);
    }
}
