package org.testcontainers.containers.rabbitmq.admin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

/**
 * Declares a permission.
 *
 * <p>The vhost defaults to the default vhost ({@value #DEFAULT_VHOST}),
 * and no configure/write/read access.</p>
 */
public class DeclarePermissionCommand extends DeclareCommand<DeclarePermissionCommand> {

    public static final String NO_ACCESS = "";

    private final String user;

    private String configure;

    private String write;

    private String read;

    public DeclarePermissionCommand(String user) {
        super("permission");
        this.user = Objects.requireNonNull(user, "user must not be null");

        // Set the default vhost so that user's don't need to explicitly configure it
        vhost(DEFAULT_VHOST);

        // Set the default permissions to grant no access
        configure = NO_ACCESS;
        write = NO_ACCESS;
        read = NO_ACCESS;
    }

    @Override
    public DeclarePermissionCommand vhost(String vhost) {
        return super.vhost(Objects.requireNonNull(vhost, "vhost must not be null"));
    }

    public DeclarePermissionCommand configure(String configure) {
        this.configure = Objects.requireNonNull(configure, "configure must not be null");
        return self();
    }

    public DeclarePermissionCommand write(String write) {
        this.write = Objects.requireNonNull(write, "write must not be null");
        return self();
    }

    public DeclarePermissionCommand read(String read) {
        this.read = Objects.requireNonNull(read, "read must not be null");
        return self();
    }

    @Override
    protected @NotNull List<String> getCliOptions() {
        // Do not include --vhost CLI option, since vhost CLI argument is used instead.
        return Collections.emptyList();
    }

    @Override
    @NotNull
    protected List<String> getCliArguments() {
        return Arrays.asList(
            cliArgument("vhost", getVhost()),
            cliArgument("user", user),
            cliArgument("configure", configure),
            cliArgument("read", read),
            cliArgument("write", write));
    }
}
