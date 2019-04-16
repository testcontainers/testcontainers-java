package org.testcontainers.vault;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class VaultClientTest {

    private static final int VAULT_PORT = 8200; //using non-default port to show other ports can be passed besides 8200

    private static final String VAULT_TOKEN = "my-root-token";

    @ClassRule
    public static VaultContainer vaultContainer = new VaultContainer<>("vault:1.1.1")
        .withVaultToken(VAULT_TOKEN)
        .withVaultPort(VAULT_PORT)
        .waitingFor(Wait.forHttp("/v1/secret/not_exists").forStatusCode(400));

    @Test
    public void writeAndReadMultipleValues() throws VaultException {
        final VaultConfig config = new VaultConfig()
            .address("http://" + vaultContainer.getContainerIpAddress() + ":" + vaultContainer.getMappedPort(VAULT_PORT))
            .token(VAULT_TOKEN)
            .build();

        final Vault vault = new Vault(config);

        final Map<String, Object> secrets = new HashMap<>();
        secrets.put("value", "world");
        secrets.put("other_value", "another world");

        // Write operation
        final LogicalResponse writeResponse = vault.logical()
            .write("secret/hello", secrets);

        assertThat(writeResponse.getRestResponse().getStatus()).isEqualTo(200);

        // Read operation
        final Map<String, String> value = vault.logical()
            .read("secret/hello")
            .getData();


        assertThat(value)
            .containsEntry("value", "world")
            .containsEntry("other_value", "another world");

    }
}
