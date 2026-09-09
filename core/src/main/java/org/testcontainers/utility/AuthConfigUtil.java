package org.testcontainers.utility;

import com.github.dockerjava.api.model.AuthConfig;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

/**
 * Utility methods for safely representing {@link AuthConfig} objects as strings.
 * <p>
 * This class masks sensitive authentication information such as passwords,
 * authentication tokens, and registry tokens before including them in the
 * returned string. It is intended for logging and debugging purposes where
 * exposing secrets would be a security risk.
 */
@UtilityClass
public class AuthConfigUtil {

    /**
     * Returns a safe string representation of the given {@link AuthConfig}.
     * <p>
     * Sensitive fields including the password, authentication value, and
     * registry token are replaced with an obfuscated placeholder, while
     * non-sensitive fields such as the username, email, and registry address
     * are included in the output. If the supplied configuration is
     * {@code null}, the string {@code "null"} is returned.
     *
     * @param authConfig the authentication configuration to represent safely;
     *                   may be {@code null}
     * @return a string representation with sensitive values hidden, or
     *         {@code "null"} if {@code authConfig} is {@code null}
     */
    public static String toSafeString(AuthConfig authConfig) {
        if (authConfig == null) {
            return "null";
        }

        return MoreObjects
            .toStringHelper(authConfig)
            .add("username", authConfig.getUsername())
            .add("password", obfuscated(authConfig.getPassword()))
            .add("auth", obfuscated(authConfig.getAuth()))
            .add("email", authConfig.getEmail())
            .add("registryAddress", authConfig.getRegistryAddress())
            .add("registryToken", obfuscated(authConfig.getRegistrytoken()))
            .toString();
    }

    /**
     * Returns an obfuscated representation of a potentially sensitive value.
     * <p>
     * If the value is {@code null} or empty, the string {@code "blank"} is
     * returned. Otherwise, the actual value is concealed by returning
     * {@code "hidden non-blank value"}.
     *
     * @param value the value to obfuscate
     * @return {@code "blank"} if the value is null or empty; otherwise
     *         {@code "hidden non-blank value"}
     */
    @NotNull
    private static String obfuscated(String value) {
        return Strings.isNullOrEmpty(value) ? "blank" : "hidden non-blank value";
    }
}
