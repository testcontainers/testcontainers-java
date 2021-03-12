package org.testcontainers.vault;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.vault.VaultTestImages.VAULT_IMAGE;

public class VaultClientTest {

    private static final String VAULT_TOKEN = "my-root-token";

    @Test
    public void writeAndReadMultipleValues() throws VaultException {
        try (
            VaultContainer<?> vaultContainer = new VaultContainer<>(VAULT_IMAGE)
                    .withVaultToken(VAULT_TOKEN)
        ) {

            vaultContainer.start();

            final VaultConfig config = new VaultConfig()
                .address("http://" + vaultContainer.getHost() + ":" + vaultContainer.getFirstMappedPort())
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
}
