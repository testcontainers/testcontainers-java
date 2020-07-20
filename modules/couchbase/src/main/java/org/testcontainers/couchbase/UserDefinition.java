/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Aaron Whiteside
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
