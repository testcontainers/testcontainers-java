package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

/**
 * @author Tomasz Forys
 */
public class SolaceContainerTestProperties {

    public static final DockerImageName IMAGE_NAME = DockerImageName.parse("solace/solace-pubsub-standard");

    public static final String TAG = "10.2";

    public static final String MESSAGE = "HelloWorld";

    public static final String TOPIC_NAME = "Topic/ActualTopic";

    public static DockerImageName getImageName() {
        return IMAGE_NAME.withTag(TAG);
    }
}
