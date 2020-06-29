package org.testcontainers.vault;

import org.testcontainers.utility.DockerImageName;

public interface VaultTestImages {
    DockerImageName VAULT_IMAGE = new DockerImageName("vault:1.1.3");
}
