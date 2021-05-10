package org.testcontainers.vault;

import org.testcontainers.utility.DockerImageName;

public interface VaultTestImages {
    DockerImageName VAULT_IMAGE = DockerImageName.parse("vault:1.1.3");
}
