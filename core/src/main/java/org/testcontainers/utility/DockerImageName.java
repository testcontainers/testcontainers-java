package org.testcontainers.utility;


import com.google.common.net.HostAndPort;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

@EqualsAndHashCode(exclude = "rawName")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DockerImageName {

    /* Regex patterns used for validation */
    private static final String ALPHA_NUMERIC = "[a-z0-9]+";
    private static final String SEPARATOR = "([\\.]{1}|_{1,2}|-+)";
    private static final String REPO_NAME_PART = ALPHA_NUMERIC + "(" + SEPARATOR + ALPHA_NUMERIC + ")*";
    private static final Pattern REPO_NAME = Pattern.compile(REPO_NAME_PART + "(/" + REPO_NAME_PART + ")*");

    private final String rawName;
    private final String registry;
    private final String repo;
    @NotNull private final Versioning versioning;

    /**
     * Parses a docker image name from a provided string.
     *
     * @param fullImageName in standard Docker format, e.g. <code>name:tag</code>,
     *                      <code>some.registry/path/name:tag</code>,
     *                      <code>some.registry/path/name@sha256:abcdef...</code>, etc.
     */
    public static DockerImageName parse(String fullImageName) {
        return new DockerImageName(fullImageName);
    }

    /**
     * Parses a docker image name from a provided string.
     *
     * @param fullImageName in standard Docker format, e.g. <code>name:tag</code>,
     *                      <code>some.registry/path/name:tag</code>,
     *                      <code>some.registry/path/name@sha256:abcdef...</code>, etc.
     * @deprecated use {@link DockerImageName#parse(String)} instead
     */
    @Deprecated
    public DockerImageName(String fullImageName) {
        this.rawName = fullImageName;
        final int slashIndex = fullImageName.indexOf('/');

        String remoteName;
        if (slashIndex == -1 ||
            (!fullImageName.substring(0, slashIndex).contains(".") &&
             !fullImageName.substring(0, slashIndex).contains(":") &&
             !fullImageName.substring(0, slashIndex).equals("localhost"))) {
            registry = "";
            remoteName = fullImageName;
        } else {
            registry = fullImageName.substring(0, slashIndex);
            remoteName = fullImageName.substring(slashIndex + 1);
        }

        if (remoteName.contains("@sha256:")) {
            repo = remoteName.split("@sha256:")[0];
            versioning = new Sha256Versioning(remoteName.split("@sha256:")[1]);
        } else if (remoteName.contains(":")) {
            repo = remoteName.split(":")[0];
            versioning = new TagVersioning(remoteName.split(":")[1]);
        } else {
            repo = remoteName;
            versioning = new TagVersioning("latest");
        }
    }

    /**
     * Parses a docker image name from a provided string, and uses a separate provided version.
     *
     * @param nameWithoutTag in standard Docker format, e.g. <code>name</code>,
     *                       <code>some.registry/path/name</code>,
     *                       <code>some.registry/path/name</code>, etc.
     * @param version        a docker image version identifier, either as a tag or sha256 checksum, e.g.
     *                       <code>tag</code>,
     *                       <code>sha256:abcdef...</code>.
     * @deprecated use {@link DockerImageName#parse(String)}.{@link DockerImageName#withTag(String)} instead
     */
    @Deprecated
    public DockerImageName(String nameWithoutTag, @NotNull String version) {
        this.rawName = nameWithoutTag;
        final int slashIndex = nameWithoutTag.indexOf('/');

        String remoteName;
        if (slashIndex == -1 ||
            (!nameWithoutTag.substring(0, slashIndex).contains(".") &&
             !nameWithoutTag.substring(0, slashIndex).contains(":") &&
             !nameWithoutTag.substring(0, slashIndex).equals("localhost"))) {
            registry = "";
            remoteName = nameWithoutTag;
        } else {
            registry = nameWithoutTag.substring(0, slashIndex);
            remoteName = nameWithoutTag.substring(slashIndex + 1);
        }

        if (version.startsWith("sha256:")) {
            repo = remoteName;
            versioning = new Sha256Versioning(version.replace("sha256:", ""));
        } else {
            repo = remoteName;
            versioning = new TagVersioning(version);
        }
    }

    /**
     * @return the unversioned (non 'tag') part of this name
     */
    public String getUnversionedPart() {
        if (!"".equals(registry)) {
            return registry + "/" + repo;
        } else {
            return repo;
        }
    }

    /**
     * @return the versioned part of this name (tag or sha256)
     */
    public String getVersionPart() {
        return versioning.toString();
    }

    /**
     * @return canonical name for the image
     */
    public String asCanonicalNameString() {
        return getUnversionedPart() + versioning.getSeparator() + versioning.toString();
    }

    @Override
    public String toString() {
        return asCanonicalNameString();
    }

    /**
     * Is the image name valid?
     *
     * @throws IllegalArgumentException if not valid
     */
    public void assertValid() {
        HostAndPort.fromString(registry);
        if (!REPO_NAME.matcher(repo).matches()) {
            throw new IllegalArgumentException(repo + " is not a valid Docker image name (in " + rawName + ")");
        }
        if (!versioning.isValid()) {
            throw new IllegalArgumentException(versioning + " is not a valid image versioning identifier (in " + rawName + ")");
        }
    }

    public String getRegistry() {
        return registry;
    }

    public DockerImageName withTag(final String newTag) {
        return new DockerImageName(rawName, registry, repo, new TagVersioning(newTag));
    }

    private interface Versioning {
        boolean isValid();

        String getSeparator();
    }

    @Data
    private static class TagVersioning implements Versioning {
        public static final String TAG_REGEX = "[\\w][\\w\\.\\-]{0,127}";
        private final String tag;

        TagVersioning(String tag) {
            this.tag = tag;
        }

        @Override
        public boolean isValid() {
            return tag.matches(TAG_REGEX);
        }

        @Override
        public String getSeparator() {
            return ":";
        }

        @Override
        public String toString() {
            return tag;
        }
    }

    @Data
    private static class Sha256Versioning implements Versioning {
        public static final String HASH_REGEX = "[0-9a-fA-F]{32,}";
        private final String hash;

        Sha256Versioning(String hash) {
            this.hash = hash;
        }

        @Override
        public boolean isValid() {
            return hash.matches(HASH_REGEX);
        }

        @Override
        public String getSeparator() {
            return "@";
        }

        @Override
        public String toString() {
            return "sha256:" + hash;
        }
    }
}
