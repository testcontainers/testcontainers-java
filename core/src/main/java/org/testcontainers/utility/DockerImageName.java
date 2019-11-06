package org.testcontainers.utility;


import com.google.common.net.HostAndPort;
import lombok.Data;

import java.util.regex.Pattern;

import static java.lang.String.format;

@Data
public final class DockerImageName {

    private static final Pattern REPO_NAME = getRepoNamePattern();

    private final String registry;
    private final String repo;
    private final Versioning versioning;

    public DockerImageName(String name) {
        RegistryWithRemote registryWithRemote = RegistryWithRemote.from(name);
        RepositoryWithVersioning repositoryWithVersioning = RepositoryWithVersioning.from(registryWithRemote.getRemoteName());

        registry = registryWithRemote.getRegistry();
        repo = repositoryWithVersioning.getRepository();
        versioning = repositoryWithVersioning.getVersioning();

        assertValid(name);
    }

    public DockerImageName(String name, String tag) {
        RegistryWithRemote registryWithRemote = RegistryWithRemote.from(name);

        registry = registryWithRemote.getRegistry();
        repo = registryWithRemote.getRemoteName();
        versioning = Versioning.from(tag);

        assertValid(name);
    }

    public String getRegistry() {
        return registry;
    }

    /**
     * @return the unversioned (non 'tag') part of this name
     */
    public String getUnversionedPart() {
        return "".equals(registry) ? repo : registry + "/" + repo;
    }

    /**
     * @return the versioned part of this name (tag or sha256)
     */
    public String getVersionPart() {
        return versioning.toString();
    }

    @Override
    public String toString() {
        return versioning == null
            ? getUnversionedPart()
            : format("%s%s%s", getUnversionedPart(), versioning.getSeparator(), versioning.toString());
    }

    /**
     * Is the image name valid?
     *
     * @throws IllegalArgumentException if not valid
     */
    private void assertValid(String rawName) {
        HostAndPort.fromString(registry);
        if (!REPO_NAME.matcher(repo).matches()) {
            throw new IllegalArgumentException(format("%s is not a valid Docker image name (in %s)", repo, rawName));
        }
        if (versioning == null) {
            throw new IllegalArgumentException(format("No image tag was specified in docker image name (%s). Please provide a tag; this may be 'latest' or a specific version", rawName));
        }
        if (!versioning.isValid()) {
            throw new IllegalArgumentException(format("%s is not a valid image versioning identifier (in %s)", versioning, rawName));
        }
    }

    private static Pattern getRepoNamePattern() {
        String alphaNumeric = "[a-z0-9]+";
        String separator = "([\\.]{1}|_{1,2}|-+)";
        String repoNamePart = format("%s(%s%s)*", alphaNumeric, separator, alphaNumeric);

        return Pattern.compile(format("%s(/%s)*", repoNamePart, repoNamePart));
    }
}
