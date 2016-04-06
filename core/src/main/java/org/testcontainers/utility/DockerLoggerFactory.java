package org.testcontainers.utility;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DockerLoggerFactory {

    public static Logger getLogger(String dockerImageName) {
        if ("UTF-8".equals(System.getProperty("file.encoding"))) {
            return LoggerFactory.getLogger("\uD83D\uDC33 [" + dockerImageName + "]");
        } else {
            return LoggerFactory.getLogger("docker[" + dockerImageName + "]");
        }
    }
}
