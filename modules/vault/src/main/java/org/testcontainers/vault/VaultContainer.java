package org.testcontainers.vault;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.traits.LinkableContainer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.dockerjava.api.model.Capability.IPC_LOCK;


/**
 * GenericContainer subclass for Vault specific configuration and features. The main feature is the
 * withSecretInVault method, where users can specify which secrets to be pre-loaded into Vault for
 * their specific test scenario.
 *
 * Other helpful features include the withVaultPort, and withVaultToken methods for convenience.
 */
public class VaultContainer<SELF extends VaultContainer<SELF>> extends GenericContainer<SELF>
        implements LinkableContainer {

    private static final String VAULT_PORT = "8200";

    private boolean vaultPortRequested = false;

    private Map<String, List<String>> secretsMap = new HashMap<>();

    public VaultContainer() {
        this("vault:0.7.0");
    }

    public VaultContainer(String dockerImageName) {
        super(dockerImageName);
    }

    @Override
    protected void configure() {
        setStartupAttempts(3);
        withCreateContainerCmdModifier(cmd -> cmd.withCapAdd(IPC_LOCK));
        if(!isVaultPortRequested()){
            withEnv("VAULT_ADDR", "http://0.0.0.0:" + VAULT_PORT);
        }
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        addSecrets();
    }

    private void addSecrets() {
        if(!secretsMap.isEmpty()){
            try {
                this.execInContainer(buildExecCommand(secretsMap)).getStdout().contains("Success");
            }
            catch (IOException | InterruptedException e) {
                logger().error("Failed to add these secrets {} into Vault via exec command. Exception message: {}", secretsMap, e.getMessage());
            }
        }
    }

    private String[] buildExecCommand(Map<String, List<String>> map) {
        StringBuilder stringBuilder = new StringBuilder();
        map.forEach((path, secrets) -> {
            stringBuilder.append(" && vault write " + path);
            secrets.forEach(item -> stringBuilder.append(" " + item));
        });
        return new String[] { "/bin/sh", "-c", stringBuilder.toString().substring(4)};
    }

    /**
     * Sets the Vault root token for the container so application tests can source secrets using the token
     *
     * @param token the root token value to set for Vault.
     * @return this
     */
    public SELF withVaultToken(String token) {
        withEnv("VAULT_DEV_ROOT_TOKEN_ID", token);
        withEnv("VAULT_TOKEN", token);
        return self();
    }

    /**
     * Sets the Vault port in the container as well as the port bindings for the host to reach the container over HTTP.
     *
     * @param port the port number you want to have the Vault container listen on for tests.
     * @return this
     */
    public SELF withVaultPort(int port){
        setVaultPortRequested(true);
        String vaultPort = String.valueOf(port);
        withEnv("VAULT_ADDR", "http://0.0.0.0:" + VAULT_PORT);
        setPortBindings(Arrays.asList(vaultPort + ":" + VAULT_PORT));
        return self();
    }

    /**
     * Pre-loads secrets into Vault container. User may specify one or more secrets and all will be added to each path
     * that is specified. Thus this can be called more than once for multiple paths to be added to Vault.
     *
     * The secrets are added to vault directly after the container is up via the
     * {@link #addSecrets() addSecrets}, called from {@link #containerIsStarted(InspectContainerResponse) containerIsStarted}
     *
     * @param path specific Vault path to store specified secrets
     * @param firstSecret first secret to add to specifed path
     * @param remainingSecrets var args list of secrets to add to specified path
     * @return this
     */
    public SELF withSecretInVault(String path, String firstSecret, String... remainingSecrets) {
        List<String> list = new ArrayList<>();
        list.add(firstSecret);
        for(String secret : remainingSecrets) {
            list.add(secret);
        }
        if (secretsMap.containsKey(path)) {
            list.addAll(list);
        }
        secretsMap.putIfAbsent(path,list);
        return self();
    }

    private void setVaultPortRequested(boolean vaultPortRequested) {
        this.vaultPortRequested = vaultPortRequested;
    }

    private boolean isVaultPortRequested() {
        return vaultPortRequested;
    }
}