package org.testcontainers.valkey;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Testcontainers implementation for Valkey.
 * <p>
 * Supported image: {@code valkey}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>Server: 6379</li>
 * </ul>
 */
public class ValkeyContainer extends GenericContainer<ValkeyContainer> {

    @AllArgsConstructor
    @Getter
    private static class SnapshottingSettings {

        int seconds;

        int changedKeys;
    }

    private static final DockerImageName DEFAULT_IMAGE = DockerImageName.parse("valkey/valkey:8.1");

    private static final String DEFAULT_CONFIG_FILE = "/usr/local/valkey.conf";

    private static final int CONTAINER_PORT = 6379;

    private String username;

    private String password;

    private String persistenceVolume;

    private String initialImportScriptFile;

    private String configFile;

    private ValkeyLogLevel logLevel;

    private SnapshottingSettings snapshottingSettings;

    public ValkeyContainer() {
        this(DEFAULT_IMAGE);
    }

    public ValkeyContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public ValkeyContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        withExposedPorts(CONTAINER_PORT);
        withStartupTimeout(Duration.ofMinutes(2));
        waitingFor(Wait.forLogMessage(".*Ready to accept connections.*", 1));
    }

    public ValkeyContainer withUsername(String username) {
        this.username = username;
        return this;
    }

    public ValkeyContainer withPassword(String password) {
        this.password = password;
        return this;
    }

    /**
     * Sets a host path to be mounted as a volume for Valkey persistence. The path must exist on the
     * host system. Valkey will store its data in this directory.
     */
    public ValkeyContainer withPersistenceVolume(String persistenceVolume) {
        this.persistenceVolume = persistenceVolume;
        return this;
    }

    /**
     * Sets an initial import script file to be executed via the Valkey CLI after startup.
     * <p>
     * Example line of an import script file: SET key1 "value1"
     */
    public ValkeyContainer withInitialData(String initialImportScriptFile) {
        this.initialImportScriptFile = initialImportScriptFile;
        return this;
    }

    /**
     * Sets the log level for the valkey server process.
     */
    public ValkeyContainer withLogLevel(ValkeyLogLevel logLevel) {
        this.logLevel = logLevel;
        return this;
    }

    /**
     * Sets the snapshotting configuration for the valkey server process. You can configure Valkey
     * to have it save the dataset every N seconds if there are at least M changes in the dataset.
     * This method allows Valkey to benefit from copy-on-write semantics.
     *
     * @see <a href="https://valkey.io/topics/persistence/#snapshotting"/>
     */
    public ValkeyContainer withSnapshotting(int seconds, int changedKeys) {
        Preconditions.checkArgument(seconds > 0, "seconds must be greater than 0");
        Preconditions.checkArgument(changedKeys > 0, "changedKeys must be non-negative");

        this.snapshottingSettings = new SnapshottingSettings(seconds, changedKeys);
        return this;
    }

    /**
     * Sets the config file to be used for the Valkey container.
     */
    public ValkeyContainer withConfigFile(String configFile) {
        this.configFile = configFile;

        return this;
    }

    @Override
    public void start() {
        List<String> command = new ArrayList<>();
        command.add("valkey-server");

        if (configFile != null && !configFile.isEmpty()) {
            withCopyToContainer(MountableFile.forHostPath(configFile), DEFAULT_CONFIG_FILE);
            command.add(DEFAULT_CONFIG_FILE);
        }

        if (password != null && !password.isEmpty()) {
            command.add("--requirepass");
            command.add(password);

            if (username != null && !username.isEmpty()) {
                command.add("--user " + username + " on >" + password + " ~* +@all");
            }
        }

        if (persistenceVolume != null && !persistenceVolume.isEmpty()) {
            command.addAll(Arrays.asList("--appendonly", "yes"));
            withFileSystemBind(persistenceVolume, "/data");
        }

        if (snapshottingSettings != null) {
            command.addAll(
                Arrays.asList("--save", snapshottingSettings.getSeconds() + " " + snapshottingSettings.getChangedKeys())
            );
        }

        if (logLevel != null) {
            command.addAll(Arrays.asList("--loglevel", logLevel.getLevel()));
        }

        if (initialImportScriptFile != null && !initialImportScriptFile.isEmpty()) {
            withCopyToContainer(MountableFile.forHostPath(initialImportScriptFile), "/tmp/import.valkey");
            withCopyToContainer(MountableFile.forClasspathResource("import.sh"), "/tmp/import.sh");
        }

        withCommand(command.toArray(new String[0]));

        super.start();

        evaluateImportScript();
    }

    public int getPort() {
        return getMappedPort(CONTAINER_PORT);
    }

    /**
     * Executes a command in the Valkey CLI inside the container.
     */
    public String executeCli(String cmd, String... flags) {
        try {
            List<String> args = new ArrayList<>();
            args.add("redis-cli");

            if (password != null && !password.isEmpty()) {
                args.addAll(
                    username != null && !username.isEmpty()
                        ? Arrays.asList("--user", username, "--pass", password)
                        : Arrays.asList("--pass", password)
                );
            }

            args.add(cmd);
            args.addAll(Arrays.asList(flags));

            ExecResult result = execInContainer(args.toArray(new String[0]));
            if (result.getExitCode() != 0) {
                throw new RuntimeException(result.getStdout() + result.getStderr());
            }

            return result.getStdout();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String createConnectionUrl() {
        String userInfo = null;
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            userInfo = username + ":" + password;
        } else if (password != null && !password.isEmpty()) {
            userInfo = ":" + password;
        }

        try {
            URI uri = new URI("redis", userInfo, getHost(), getPort(), null, null, null);
            return uri.toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to build Redis URI", e);
        }
    }

    private void evaluateImportScript() {
        if (initialImportScriptFile == null || initialImportScriptFile.isEmpty()) {
            return;
        }

        try {
            ExecResult result = execInContainer("/bin/sh", "/tmp/import.sh", password != null ? password : "");

            if (result.getExitCode() != 0 || result.getStdout().contains("ERR")) {
                throw new RuntimeException("Could not import initial data: " + result.getStdout());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
