package org.testcontainers.containers.rabbitmq.admin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

/**
 * Declares a user.
 *
 * <p>Password defaults to the empty string.</p>
 */
public class DeclareUserCommand extends DeclareCommand<DeclareUserCommand> {

    public static final String TAG_SEPARATOR = ",";

    private final String name;

    private String password = "";

    private String passwordHash;

    private String hashingAlgorithm;

    private final Set<String> tags = new HashSet<>();

    public DeclareUserCommand(String name) {
        super("user");
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    /**
     * Sets the password, and clears the password hash.
     * @param password the password
     * @return this command
     */
    public DeclareUserCommand password(String password) {
        this.password = Objects.requireNonNull(password);
        this.passwordHash = null;
        this.hashingAlgorithm = null;
        return self();
    }

    /**
     * Sets the password hash, and clears the password.
     *
     * @param passwordHash the password hash
     * @return this command
     */
    public DeclareUserCommand passwordHash(String passwordHash) {
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.password = null;
        return self();
    }

    /**
     * Sets the hashing algorithm for the password hash, and clears the password.
     *
     * @param hashingAlgorithm the hashing algorithm.
     * @return this command
     */
    public DeclareUserCommand hashingAlgorithm(String hashingAlgorithm) {
        this.hashingAlgorithm = Objects.requireNonNull(hashingAlgorithm);
        this.password = null;
        return self();
    }


    /**
     * Adds the given tag to the user.
     * @param tag the tag to add
     * @return this command
     */
    public DeclareUserCommand tag(String tag) {
        if (tag.contains(TAG_SEPARATOR)) {
            throw new IllegalArgumentException(String.format("Tag '%s' must not contain '%s'", tag, TAG_SEPARATOR));
        }
        this.tags.add(tag);
        return self();
    }

    /**
     * Adds the given tags to the user.
     * @param tags the tags to add
     * @return this command
     */
    public DeclareUserCommand tags(Set<String> tags) {
        tags.forEach(this::tag);
        return self();
    }

    @Override
    @NotNull
    protected List<String> getCliArguments() {

        List<String> cliArguments = new ArrayList<>();
        cliArguments.add(cliArgument("name", name));

        if (password != null) {
            cliArguments.add(cliArgument("password", password));
        }
        if (passwordHash != null) {
            cliArguments.add(cliArgument("password_hash", passwordHash));
        }
        if (hashingAlgorithm != null) {
            cliArguments.add(cliArgument("hashing_algorithm", hashingAlgorithm));
        }
        cliArguments.add(cliArgument("tags", String.join(TAG_SEPARATOR, tags)));

        return cliArguments;
    }
}
