package org.testcontainers.utility;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DockerLoggerFactory {

    public static Logger getLogger(String dockerImageName) {

        final String abbreviatedName;
        if (dockerImageName.contains("@sha256")) {
            abbreviatedName = dockerImageName.substring(0, dockerImageName.indexOf("@sha256") + 14) + "...";
        } else {
            abbreviatedName = dockerImageName;
        }

        if ("UTF-8".equals(System.getProperty("file.encoding"))) {
            return LoggerFactory.getLogger("\uD83D\uDC33 [" + abbreviatedName + "]");
        } else {
            return LoggerFactory.getLogger("docker[" + abbreviatedName + "]");
        }
    }
}
