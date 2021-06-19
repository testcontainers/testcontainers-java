package org.testcontainers.couchbase;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NonNull;
import lombok.Setter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Allows to configure the properties of a user that should be created.
 */
@Data
@Setter(AccessLevel.NONE)
public class UserDefinition {

    private final String username;
    private String password;
    private final Set<String> roles = new HashSet<>();
    private String name;

    public UserDefinition(@NonNull final String username) {
        this.username = username;
    }

    /**
     * Sets the password for the defined user.
     *
     * @param password the password to set for the user.
     * @return this {@link UserDefinition} for chaining purposes.
     */
    public UserDefinition withPassword(@NonNull final String password) {
        this.password = password;
        return this;
    }

    /**
     * Sets the roles for the defined user.
     *
     * @param roles the roles for the user.
     * @return this {@link UserDefinition} for chaining purposes.
     */
    public UserDefinition withRoles(@NonNull final String...roles) {
        this.roles.addAll(Arrays.asList(roles));
        return this;
    }

    /**
     * Sets the name of the defined user.
     *
     * @param name the users actual name.
     * @return this {@link UserDefinition} for chaining purposes.
     */
    public UserDefinition withName(@NonNull final String name) {
        this.name = name;
        return this;
    }
}
