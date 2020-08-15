package org.testcontainers.utility;


import com.google.common.net.HostAndPort;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

@EqualsAndHashCode(exclude = "rawName")
public final class DockerImageName {

    /* Regex patterns used for validation */
    private static final String ALPHA_NUMERIC = "[a-z0-9]+";
    private static final String SEPARATOR = "([.]|_{1,2}|-+)";
    private static final String REPO_NAME_PART = ALPHA_NUMERIC + "(" + SEPARATOR + ALPHA_NUMERIC + ")*";
    private static final Pattern REPO_NAME = Pattern.compile(REPO_NAME_PART + "(/" + REPO_NAME_PART + ")*");

    private final String rawName;
    private final String registry;
    private final String repo;
    private final Versioning versioning;
    @Nullable
    private final DockerImageName compatibleSubstituteFor;

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
            versioning = new Versioning.Sha256Versioning(remoteName.split("@sha256:")[1]);
        } else if (remoteName.contains(":")) {
            repo = remoteName.split(":")[0];
            versioning = new Versioning.TagVersioning(remoteName.split(":")[1]);
        } else {
            repo = remoteName;
            versioning = Versioning.TagVersioning.LATEST;
        }

        compatibleSubstituteFor = null;
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
            versioning = new Versioning.Sha256Versioning(version.replace("sha256:", ""));
        } else {
            repo = remoteName;
            versioning = new Versioning.TagVersioning(version);
        }

        compatibleSubstituteFor = null;
    }

    private DockerImageName(String rawName,
                            String registry,
                            String repo,
                            @Nullable Versioning versioning,
                            @Nullable DockerImageName compatibleSubstituteFor) {

        this.rawName = rawName;
        this.registry = registry;
        this.repo = repo;
        this.versioning = versioning;
        this.compatibleSubstituteFor = compatibleSubstituteFor;
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
        return versioning == null ? "latest" : versioning.toString();
    }

    /**
     * @return canonical name for the image
     */
    public String asCanonicalNameString() {
        return getUnversionedPart() + (versioning == null ? ":" : versioning.getSeparator()) + getVersionPart();
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
        //noinspection UnstableApiUsage
        HostAndPort.fromString(registry); // return value ignored - this throws if registry is not a valid host:port string
        if (!REPO_NAME.matcher(repo).matches()) {
            throw new IllegalArgumentException(repo + " is not a valid Docker image name (in " + rawName + ")");
        }
        if (versioning != null && !versioning.isValid()) {
            throw new IllegalArgumentException(versioning + " is not a valid image versioning identifier (in " + rawName + ")");
        }
    }

    public String getRegistry() {
        return registry;
    }

    /**
     * @param newTag version tag for the copy to use
     * @return an immutable copy of this {@link DockerImageName} with the new version tag
     */
    public DockerImageName withTag(final String newTag) {
        return new DockerImageName(rawName, registry, repo, new Versioning.TagVersioning(newTag), compatibleSubstituteFor);
    }

    /**
     * Declare that this {@link DockerImageName} is a compatible substitute for another image - i.e. that this image
     * behaves as the other does, and is compatible with Testcontainers' assumptions about the other image.
     *
     * @param otherImageName the image name of the other image
     * @return an immutable copy of this {@link DockerImageName} with the compatibility declaration attached.
     */
    public DockerImageName asCompatibleSubstituteFor(String otherImageName) {
        return asCompatibleSubstituteFor(DockerImageName.parse(otherImageName));
    }

    /**
     * Declare that this {@link DockerImageName} is a compatible substitute for another image - i.e. that this image
     * behaves as the other does, and is compatible with Testcontainers' assumptions about the other image.
     *
     * @param otherImageName the image name of the other image
     * @return an immutable copy of this {@link DockerImageName} with the compatibility declaration attached.
     */
    public DockerImageName asCompatibleSubstituteFor(DockerImageName otherImageName) {
        return new DockerImageName(rawName, registry, repo, versioning, otherImageName);
    }

    /**
     * Test whether this {@link DockerImageName} has declared compatibility with another image (set using
     * {@link DockerImageName#asCompatibleSubstituteFor(String)} or
     * {@link DockerImageName#asCompatibleSubstituteFor(DockerImageName)}.
     * <p>
     * If a version tag part is present in the <code>other</code> image name, the tags must exactly match, unless it
     * is 'latest'. If a version part is not present in the <code>other</code> image name, the tag contents are ignored.
     *
     * @param other the other image that we are trying to test compatibility with
     * @return whether this image has declared compatibility.
     */
    public boolean isCompatibleWith(DockerImageName other) {
        // is this image already the same?
        final boolean thisRegistrySame = other.registry.equals(this.registry);
        final boolean thisRepoSame = other.repo.equals(this.repo);
        final boolean thisVersioningNotSpecifiedOrSame = other.versioning == null ||
            other.versioning.equals(Versioning.TagVersioning.LATEST) ||
            other.versioning.equals(this.versioning);

        if (thisRegistrySame && thisRepoSame && thisVersioningNotSpecifiedOrSame) {
            return true;
        }

        if (this.compatibleSubstituteFor == null) {
            return false;
        }

        return this.compatibleSubstituteFor.isCompatibleWith(other);
    }

    /**
     * Behaves as {@link DockerImageName#isCompatibleWith(DockerImageName)} but throws an exception rather than
     * returning false if a mismatch is detected.
     *
     * @param other the other image that we are trying to check compatibility with
     * @throws IllegalStateException if {@link DockerImageName#isCompatibleWith(DockerImageName)} returns false
     */
    public void assertCompatibleWith(DockerImageName other) {
        if (!this.isCompatibleWith(other)) {
            throw new IllegalStateException(
                String.format(
                    "Failed to verify that image '%s' is a compatible substitute for '%s'. This generally means that " +
                        "you are trying to use an image that Testcontainers has not been designed to use. If this is " +
                        "deliberate, and if you are confident that the image is compatible, you should declare " +
                        "compatibility in code using the `asCompatibleSubstituteFor` method. For example:\n" +
                        "   DockerImageName myImage = DockerImageName.parse(\"%s\").asCompatibleSubstituteFor(\"%s\");\n" +
                        "and then use `myImage` instead.",
                    this.rawName, other.rawName, this.rawName, other.rawName
                )
            );
        }
    }
}
