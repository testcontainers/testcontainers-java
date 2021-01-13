package org.testcontainers.vault;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.response.LogicalResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import org.testcontainers.utility.DockerImageName;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.*;

/**
 * This test shows the pattern to use the VaultContainer @ClassRule for a junit test. It also has tests that ensure
 * the secrets were added correctly by reading from Vault with the CLI, over HTTP and over Client Library.
 */
public class VaultContainerTest {

    private static final String VAULT_TOKEN = "my-root-token";

    // vaultContainer {
    @ClassRule
    public static VaultContainer<?> vaultContainer = new VaultContainer<>(DockerImageName.parse("vault:1.6.1"))
        .withVaultToken(VAULT_TOKEN)
        .withSecretInVault("secret/testing1", "top_secret=password123")
        .withSecretInVault("secret/testing2",
            "secret_one=password1",
            "secret_two=password2",
            "secret_three=password3",
            "secret_three=password3",
            "secret_four=password4")
        .withInitCommand("secrets enable transit", "write -f transit/keys/my-key");
    // }

    @Test
    // readWithCli {
    public void readFirstSecretPathWithCli() throws Exception {
        GenericContainer.ExecResult result =
            vaultContainer.execInContainer("vault", "kv", "get", "-format=json", "secret/testing1");
        final String output = result.getStdout().replaceAll("\\r?\\n", "");
        assertThat(output).contains("password123");
    }
    // }

    @Test
    public void readSecondSecretPathWithCli() throws Exception {
        GenericContainer.ExecResult result = vaultContainer.execInContainer("vault", "kv", "get", "-format=json", "secret/testing2");
        final String output = result.getStdout().replaceAll("\\r?\\n", "");
        System.out.println("output = " + output);
        assertThat(output).contains("password1");
        assertThat(output).contains("password2");
        assertThat(output).contains("password3");
        assertThat(output).contains("password4");
    }

    @Test
    // readWithHttpApi {
    public void readFirstSecretPathOverHttpApi() {
        given().
            header("X-Vault-Token", VAULT_TOKEN).
            when().
            get(vaultContainer.getVaultAddress() + "/v1/secret/data/testing1").
            then().
            assertThat().body("data.data.top_secret", equalTo("password123"));
    }
    // }

    @Test
    public void readSecondSecretPathOverHttpApi() {
        given().
            header("X-Vault-Token", VAULT_TOKEN).
            when().
            get(vaultContainer.getVaultAddress() + "/v1/secret/data/testing2").
            then().
            assertThat().body("data.data.secret_one", containsString("password1")).
            assertThat().body("data.data.secret_two", containsString("password2")).
            assertThat().body("data.data.secret_three", hasItem("password3")).
            assertThat().body("data.data.secret_four", containsString("password4"));
    }

    @Test
    public void readTransitKeyOverHttpApi() {
        given().
            header("X-Vault-Token", VAULT_TOKEN).
            when().
            get(vaultContainer.getVaultAddress() + "/v1/transit/keys/my-key").
            then().
            assertThat().body("data.name", equalTo("my-key"));
    }

    @Test
    // readWithLibrary {
    public void readFirstSecretPathOverClientLibrary() throws Exception {
        final VaultConfig config = new VaultConfig()
            .address(vaultContainer.getVaultAddress())
            .token(VAULT_TOKEN)
            .build();

        final Vault vault = new Vault(config);

        final Map<String, String> value = vault.logical()
            .read("secret/testing1")
            .getData();

        assertThat(value)
            .containsEntry("top_secret", "password123");
    }
    // }

    @Test
    public void readSecondSecretPathOverClientLibrary() throws Exception {
        final VaultConfig config = new VaultConfig()
            .address(vaultContainer.getVaultAddress())
            .token(VAULT_TOKEN)
            .build();

        final Vault vault = new Vault(config);
        final Map<String, String> value = vault.logical()
            .read("secret/testing1")
            .getData();

        assertThat(value)
            .containsEntry("secret_one", "password1")
            .containsEntry("secret_two", "password2")
            .containsEntry("secret_three", "password3")
            .containsEntry("secret_four", "password4");
    }

    @Test
    public void writeSecretOverClientLibrary() throws Exception {
        final VaultConfig config = new VaultConfig()
            .address(vaultContainer.getVaultAddress())
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
