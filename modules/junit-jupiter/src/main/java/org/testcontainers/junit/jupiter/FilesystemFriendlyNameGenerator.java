package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.extension.ExtensionContext;

import static org.junit.platform.commons.util.StringUtils.isBlank;

class FilesystemFriendlyNameGenerator {
    private static final String UNKNOWN_NAME = "unknown";
    private static final String ALLOWED_CHARACTERS_REGEX = "[^\\w -]";

    static String filesystemFriendlyNameOf(ExtensionContext context) {
        String displayName = context.getDisplayName();
        if (isBlank(displayName)) {
            return UNKNOWN_NAME;
        }

        return replaceIllegalCharacters(displayName.trim());
    }

    private static String replaceIllegalCharacters(String source) {
        return source.replaceAll(ALLOWED_CHARACTERS_REGEX, "");
    }
}
