package org.testcontainers.vault;

import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * This test shows the pattern to use the VaultContainer @ClassRule for a junit test. It also has tests that ensure
 * the secrets were added correctly by reading from Vault with the CLI and over HTTP.
 */
public class VaultContainerTest {

    private static final String VAULT_TOKEN = "my-root-token";

    @ClassRule
    public static VaultContainer<?> vaultContainer = new VaultContainer<>(VaultTestImages.VAULT_IMAGE)
        .withVaultToken(VAULT_TOKEN)
        .withSecretInVault("secret/testing1", "top_secret=password123")
        .withSecretInVault("secret/testing2",
            "secret_one=password1",
            "secret_two=password2",
            "secret_three=password3",
            "secret_three=password3",
            "secret_four=password4")
        .withInitCommand("secrets enable transit", "write -f transit/keys/my-key");

    @Test
    public void readFirstSecretPathWithCli() throws IOException, InterruptedException {
        GenericContainer.ExecResult result = vaultContainer.execInContainer("vault", "kv", "get", "-format=json", "secret/testing1");
        final String output = result.getStdout().replaceAll("\\r?\\n", "");
        assertThat(output, containsString("password123"));
    }

    @Test
    public void readSecondSecretPathWithCli() throws IOException, InterruptedException {
        GenericContainer.ExecResult result = vaultContainer.execInContainer("vault", "kv", "get", "-format=json", "secret/testing2");
        final String output = result.getStdout().replaceAll("\\r?\\n", "");
        System.out.println("output = " + output);
        assertThat(output, containsString("password1"));
        assertThat(output, containsString("password2"));
        assertThat(output, containsString("password3"));
        assertThat(output, containsString("password4"));
    }

    @Test
    public void readFirstSecretPathOverHttpApi() throws InterruptedException {
        given().
            header("X-Vault-Token", VAULT_TOKEN).
            when().
            get("http://" + getHostAndPort() + "/v1/secret/data/testing1").
            then().
            assertThat().body("data.data.top_secret", equalTo("password123"));
    }

    @Test
    public void readSecondSecretPathOverHttpApi() throws InterruptedException {
        given().
            header("X-Vault-Token", VAULT_TOKEN).
            when().
            get("http://" + getHostAndPort() + "/v1/secret/data/testing2").
            then().
            assertThat().body("data.data.secret_one", containsString("password1")).
            assertThat().body("data.data.secret_two", containsString("password2")).
            assertThat().body("data.data.secret_three", hasItem("password3")).
            assertThat().body("data.data.secret_four", containsString("password4"));
    }

    @Test
    public void readTransitKeyOverHttpApi() throws InterruptedException {
        given().
            header("X-Vault-Token", VAULT_TOKEN).
            when().
            get("http://" + getHostAndPort() + "/v1/transit/keys/my-key").
            then().
            assertThat().body("data.name", equalTo("my-key"));
    }

    private String getHostAndPort() {
        return vaultContainer.getHost() + ":" + vaultContainer.getMappedPort(8200);
    }


}
