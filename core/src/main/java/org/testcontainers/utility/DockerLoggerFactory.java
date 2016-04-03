package org.testcontainers.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerLoggerFactory {

    public static Logger getLogger(String dockerImageName) {
        if ("UTF-8".equals(System.getProperty("file.encoding"))) {
            return LoggerFactory.getLogger("\uD83D\uDC33 [" + dockerImageName + "]");
        } else {
            return LoggerFactory.getLogger("docker[" + dockerImageName + "]");
        }
    }
}
