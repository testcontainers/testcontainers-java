package org.testcontainers.utility;

public final class DockerImageName {

    public static void validate(String dockerImageName) throws IllegalArgumentException {
        int repoSeparatorIndex = dockerImageName.indexOf('/');
        int tagSeparatorIndex;
        if (repoSeparatorIndex == -1) {
            tagSeparatorIndex = dockerImageName.indexOf(':');
        } else {
            tagSeparatorIndex = dockerImageName.indexOf(':', repoSeparatorIndex);
        }
        if (tagSeparatorIndex == -1) {
            throw new IllegalArgumentException("No image tag was specified in docker image name " +
                    "(" + dockerImageName + "). Please provide a tag; this may be 'latest' or a specific version");
        }
    }

    private DockerImageName() {}
}
