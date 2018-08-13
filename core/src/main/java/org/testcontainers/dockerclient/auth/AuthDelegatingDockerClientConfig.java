package org.testcontainers.dockerclient.auth;

import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.core.DockerClientConfig;
import lombok.experimental.Delegate;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.RegistryAuthLocator;

/**
 * TODO: Javadocs
 */
public class AuthDelegatingDockerClientConfig implements DockerClientConfig {

    @Delegate(excludes = DelegateExclusions.class)
    private DockerClientConfig delegate;

    private RegistryAuthLocator authLocator;

    public AuthDelegatingDockerClientConfig(DockerClientConfig delegate) {
        this.delegate = delegate;
        this.authLocator = new RegistryAuthLocator(new AuthConfig());
    }

    public AuthConfig effectiveAuthConfig(String imageName) {
        DockerImageName parsed = new DockerImageName(imageName);
        return authLocator.lookupAuthConfig(parsed);
    }

    private interface DelegateExclusions {
        AuthConfig effectiveAuthConfig(String imageName);
    }
}
