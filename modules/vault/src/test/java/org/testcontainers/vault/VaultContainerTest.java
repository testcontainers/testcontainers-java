package org.testcontainers.vault;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.response.LogicalResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test shows the pattern to use the {@link VaultContainer} for a junit test. It also has tests that ensure
 * the secrets were added correctly by reading from Vault with the CLI, over HTTP and over Client Library.
 */
@Testcontainers
public class VaultContainerTest {

    private static final String VAULT_TOKEN = "my-root-token";

    // vaultContainer {
    @Container
    public static VaultContainer<?> vaultContainer = new VaultContainer<>("hashicorp/vault:1.13")
        .withVaultToken(VAULT_TOKEN)
        .withInitCommand(
            "secrets enable transit",
            "write -f transit/keys/my-key",
            "kv put secret/testing1 top_secret=password123",
            "kv put secret/testing2 secret_one=password1 secret_two=password2 secret_three=password3 secret_three=password3 secret_four=password4"
        );

    // }

    @Test
    public void readFirstSecretPathWithCli() throws Exception {
        GenericContainer.ExecResult result = vaultContainer.execInContainer(
            "vault",
            "kv",
            "get",
            "-format=json",
            "secret/testing1"
        );
        assertThat(result.getStdout()).contains("password123");
    }

    @Test
    public void readSecondSecretPathWithCli() throws Exception {
        GenericContainer.ExecResult result = vaultContainer.execInContainer(
            "vault",
            "kv",
            "get",
            "-format=json",
            "secret/testing2"
        );

        final String output = result.getStdout().replaceAll("\\r?\\n", "");
        System.out.println("output = " + output);
        assertThat(output).contains("password1");
        assertThat(output).contains("password2");
        assertThat(output).contains("password3");
        assertThat(output).contains("password4");
    }

    @Test
    public void readFirstSecretPathOverHttpApi() {
        Response response = given()
            .header("X-Vault-Token", VAULT_TOKEN)
            .when()
            .get(vaultContainer.getHttpHostAddress() + "/v1/secret/data/testing1")
            .thenReturn();
        assertThat(response.body().jsonPath().getString("data.data.top_secret")).isEqualTo("password123");
    }

    @Test
    public void readSecondSecretPathOverHttpApi() throws InterruptedException {
        Response response = given()
            .header("X-Vault-Token", VAULT_TOKEN)
            .when()
            .get(vaultContainer.getHttpHostAddress() + "/v1/secret/data/testing2")
            .andReturn();

        assertThat(response.body().jsonPath().getString("data.data.secret_one")).contains("password1");
        assertThat(response.body().jsonPath().getString("data.data.secret_two")).contains("password2");
        assertThat(response.body().jsonPath().getList("data.data.secret_three")).contains("password3");
        assertThat(response.body().jsonPath().getString("data.data.secret_four")).contains("password4");
    }

    @Test
    public void readTransitKeyOverHttpApi() throws InterruptedException {
        Response response = given()
            .header("X-Vault-Token", VAULT_TOKEN)
            .when()
            .get(vaultContainer.getHttpHostAddress() + "/v1/transit/keys/my-key")
            .thenReturn();

        assertThat(response.body().jsonPath().getString("data.name")).isEqualTo("my-key");
    }

    @Test
    // readWithLibrary {
    public void readFirstSecretPathOverClientLibrary() throws Exception {
        final VaultConfig config = new VaultConfig()
            .address(vaultContainer.getHttpHostAddress())
            .token(VAULT_TOKEN)
            .build();

        final Vault vault = new Vault(config);

        final Map<String, String> value = vault.logical().read("secret/testing1").getData();

        assertThat(value).containsEntry("top_secret", "password123");
    }

    // }

    @Test
    public void readSecondSecretPathOverClientLibrary() throws Exception {
        final VaultConfig config = new VaultConfig()
            .address(vaultContainer.getHttpHostAddress())
            .token(VAULT_TOKEN)
            .build();

        final Vault vault = new Vault(config);
        final Map<String, String> value = vault.logical().read("secret/testing2").getData();

        assertThat(value)
            .containsEntry("secret_one", "password1")
            .containsEntry("secret_two", "password2")
            .containsEntry("secret_three", "[\"password3\",\"password3\"]")
            .containsEntry("secret_four", "password4");
    }

    @Test
    public void writeSecretOverClientLibrary() throws Exception {
        final VaultConfig config = new VaultConfig()
            .address(vaultContainer.getHttpHostAddress())
            .token(VAULT_TOKEN)
            .build();

        final Vault vault = new Vault(config);

        final Map<String, Object> secrets = new HashMap<>();
        secrets.put("value", "world");
        secrets.put("other_value", "another world");

        // Write operation
        final LogicalResponse writeResponse = vault.logical().write("secret/hello", secrets);

        assertThat(writeResponse.getRestResponse().getStatus()).isEqualTo(200);

        // Read operation
        final Map<String, String> value = vault.logical().read("secret/hello").getData();

        assertThat(value).containsEntry("value", "world").containsEntry("other_value", "another world");
    }
}
