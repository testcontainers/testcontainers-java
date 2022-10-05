package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

class FilesystemFriendlyNameGenerator {

    private static final String UNKNOWN_NAME = "unknown";

    static String filesystemFriendlyNameOf(ExtensionContext context) {
        String contextId = context.getUniqueId();
        try {
            return (contextId == null || contextId.trim().isEmpty())
                ? UNKNOWN_NAME
                : URLEncoder.encode(contextId, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return UNKNOWN_NAME;
        }
    }
}
