package org.testcontainers.utility;

public class FakeImageSubstitutor extends ImageNameSubstitutor {
    @Override
    public DockerImageName apply(final DockerImageName original) {
        return DockerImageName.parse("transformed-" + original.asCanonicalNameString());
    }

    @Override
    protected String getDescription() {
        return "test implementation";
    }
}
