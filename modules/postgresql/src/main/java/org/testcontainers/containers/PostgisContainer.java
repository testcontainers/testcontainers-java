package org.testcontainers.containers;

public class PostgisContainer<SELF extends PostgisContainer<SELF>> extends AbstractPostgreSQLContainer<SELF> {
    public static final String NAME = "postgis";
    public static final String IMAGE = "mdillon/postgis";
    public static final String DEFAULT_TAG = "10";

    public PostgisContainer() {
        super(IMAGE, DEFAULT_TAG);
    }

    public PostgisContainer(String dockerImageName) {
        super(dockerImageName);
    }
}
