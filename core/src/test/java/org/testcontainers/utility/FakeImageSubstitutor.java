package org.testcontainers.utility;

public class FakeImageSubstitutor extends ImageNameSubstitutor {
    @Override
    public DockerImageName apply(final DockerImageName original) {
        return null;
    }

    @Override
    protected String getDescription() {
        return null;
    }
}
