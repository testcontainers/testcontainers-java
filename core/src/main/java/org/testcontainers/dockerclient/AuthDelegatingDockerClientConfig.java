package org.testcontainers.dockerclient;

import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientConfigDelegate;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.RegistryAuthLocator;

import static org.testcontainers.utility.AuthConfigUtil.toSafeString;

/**
 * Facade implementation for {@link DockerClientConfig} which overrides how authentication
 * configuration is obtained. A delegate {@link DockerClientConfig} will be called first
 * to try and obtain auth credentials, but after that {@link RegistryAuthLocator} will be
 * used to try and improve the auth resolution (e.g. using credential helpers).
 *
 * TODO move to docker-java
 */
@Slf4j
class AuthDelegatingDockerClientConfig extends DockerClientConfigDelegate {

    public AuthDelegatingDockerClientConfig(DockerClientConfig delegate) {
        super(delegate);
    }

    @Override
    public AuthConfig effectiveAuthConfig(String imageName) {
        // allow docker-java auth config to be used as a fallback
        AuthConfig fallbackAuthConfig;
        try {
            fallbackAuthConfig = super.effectiveAuthConfig(imageName);
        } catch (Exception e) {
            log.debug("Delegate call to effectiveAuthConfig failed with cause: '{}'. " +
                "Resolution of auth config will continue using RegistryAuthLocator.",
                e.getMessage());
            fallbackAuthConfig = new AuthConfig();
        }

        // try and obtain more accurate auth config using our resolution
        final DockerImageName parsed = DockerImageName.parse(imageName);
        final AuthConfig effectiveAuthConfig = RegistryAuthLocator.instance()
            .lookupAuthConfig(parsed, fallbackAuthConfig);

        log.debug("Effective auth config [{}]", toSafeString(effectiveAuthConfig));
        return effectiveAuthConfig;
    }
}
