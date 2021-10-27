package org.testcontainers.containers;

import org.jetbrains.annotations.NotNull;
import org.testcontainers.utility.DockerImageName;


/**
 * This {@link Container} is based on the official MongoDb ({@code mongo}) image from
 * <a href="https://hub.docker.com/r/_/mongo/">DockerHub</a>. If you need to use a custom MongoDB
 * image, you can provide the full image name as well.
 *
 * @author Stefan Ludwig
 */
public class MongoDbContainer extends GenericContainer<MongoDbContainer> {

    /**
     * This is the internal port on which MongoDB is running inside the container.
     * <p>
     * You can use this constant in case you want to map an explicit public port to it
     * instead of the default random port. This can be done using methods like
     * {@link #setPortBindings(java.util.List)}.
     */
    public static final int MONGODB_PORT = 27017;
    public static final String DEFAULT_IMAGE_AND_TAG = "mongo:4.0";

    /**
     * Creates a new {@link MongoDbContainer} with the {@value DEFAULT_IMAGE_AND_TAG} image.
     * @deprecated use {@link MongoDbContainer(DockerImageName)} instead
     */
    @Deprecated
    public MongoDbContainer() {
        this(DEFAULT_IMAGE_AND_TAG);
    }

    /**
     * Creates a new {@link MongoDbContainer} with the given {@code 'image'}.
     *
     * @param image the image (e.g. {@value DEFAULT_IMAGE_AND_TAG}) to use
     * @deprecated use {@link MongoDbContainer(DockerImageName)} instead
     */
    public MongoDbContainer(@NotNull String image) {
        this(DockerImageName.parse(image));
    }

    /**
     * Creates a new {@link MongoDbContainer} with the specified image.
     */
    public MongoDbContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        addExposedPort(MONGODB_PORT);
    }

    /**
     * Returns the actual public port of the internal MongoDB port ({@value MONGODB_PORT}).
     *
     * @return the public port of this container
     * @see #getMappedPort(int)
     */
    @NotNull
    public Integer getPort() {
        return getMappedPort(MONGODB_PORT);
    }

}
