package org.testcontainers.junit.jqwik;

import net.jqwik.api.lifecycle.LifecycleContext;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.platform.commons.util.StringUtils.isBlank;

class FilesystemFriendlyNameGenerator {
    private static final String UNKNOWN_NAME = "unknown";

    static String filesystemFriendlyNameOf(LifecycleContext context) {
        String contextId = context.label();
        try {
            return (isBlank(contextId))
                ? UNKNOWN_NAME
                : URLEncoder.encode(contextId, UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return UNKNOWN_NAME;
        }
    }
}
