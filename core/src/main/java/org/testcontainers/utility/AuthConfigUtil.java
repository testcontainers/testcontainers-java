package org.testcontainers.utility;

import com.github.dockerjava.api.model.AuthConfig;
import com.google.common.base.MoreObjects;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * TODO: Javadocs
 */
@UtilityClass
public class AuthConfigUtil {
    public static String toSafeString(AuthConfig authConfig) {
        if (authConfig == null) {
            return "null";
        }

        return MoreObjects.toStringHelper(authConfig)
            .add("username", authConfig.getUsername())
            .add("password", obfuscated(authConfig.getPassword()))
            .add("auth", obfuscated(authConfig.getAuth()))
            .add("email", authConfig.getEmail())
            .add("registryAddress", authConfig.getRegistryAddress())
            .add("registryToken", obfuscated(authConfig.getRegistrytoken()))
            .toString();
    }

    @NotNull
    private static String obfuscated(String value) {
        return isNullOrEmpty(value) ? "blank" : "hidden non-blank value";
    }
}
