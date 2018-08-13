package org.testcontainers.dockerclient.auth;

import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.core.DockerClientConfig;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.RegistryAuthLocator;

/**
 * TODO: Javadocs
 */
@Slf4j
public class AuthDelegatingDockerClientConfig implements DockerClientConfig {

    @Delegate(excludes = DelegateExclusions.class)
    private DockerClientConfig delegate;

    public AuthDelegatingDockerClientConfig(DockerClientConfig delegate) {
        this.delegate = delegate;
    }

    public AuthConfig effectiveAuthConfig(String imageName) {
        // allow docker-java auth config to be used as a fallback
        final AuthConfig fallbackAuthConfig = delegate.effectiveAuthConfig(imageName);

        // try and obtain more accurate auth config using our resolution
        DockerImageName parsed = new DockerImageName(imageName);
        final RegistryAuthLocator authLocator = new RegistryAuthLocator(fallbackAuthConfig);
        final AuthConfig effectiveAuthConfig = authLocator.lookupAuthConfig(parsed);

        log.debug("effective auth config [{}]", effectiveAuthConfig);
        return effectiveAuthConfig;
    }

    private interface DelegateExclusions {
        AuthConfig effectiveAuthConfig(String imageName);
    }
}
