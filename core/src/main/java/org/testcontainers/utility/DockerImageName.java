package org.testcontainers.utility;


import com.google.common.net.HostAndPort;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.regex.Pattern;

@EqualsAndHashCode
public final class DockerImageName {

    /* Regex patterns used for validation */
    private static final String ALPHA_NUMERIC = "[a-z0-9]+";
    private static final String SEPARATOR = "([\\.]{1}|_{1,2}|-+)";
    private static final String REPO_NAME_PART = ALPHA_NUMERIC + "(" + SEPARATOR + ALPHA_NUMERIC + ")*";
    private static final Pattern REPO_NAME = Pattern.compile(REPO_NAME_PART + "(/" + REPO_NAME_PART + ")*");

    private final String rawName;
    private final String registry;
    private final String repo;
    private final Versioning versioning;

    public DockerImageName(String name) {
        this.rawName = name;
        final int slashIndex = name.indexOf('/');

        String remoteName;
        if (slashIndex == -1 ||
            (!name.substring(0, slashIndex).contains(".") &&
                !name.substring(0, slashIndex).contains(":") &&
                !name.substring(0, slashIndex).equals("localhost"))) {
            registry = "";
            remoteName = name;
        } else {
            registry = name.substring(0, slashIndex);
            remoteName = name.substring(slashIndex + 1);
        }

        if (remoteName.contains("@sha256:")) {
            repo = remoteName.split("@sha256:")[0];
            versioning = new Sha256Versioning(remoteName.split("@sha256:")[1]);
        } else if (remoteName.contains(":")) {
            repo = remoteName.split(":")[0];
            versioning = new TagVersioning(remoteName.split(":")[1]);
        } else {
            repo = remoteName;
            versioning = null;
        }
    }

    public DockerImageName(String name, String tag) {
        this.rawName = name;
        final int slashIndex = name.indexOf('/');

        String remoteName;
        if (slashIndex == -1 ||
            (!name.substring(0, slashIndex).contains(".") &&
                !name.substring(0, slashIndex).contains(":") &&
                !name.substring(0, slashIndex).equals("localhost"))) {
            registry = "";
            remoteName = name;
        } else {
            registry = name.substring(0, slashIndex - 1);
            remoteName = name.substring(slashIndex + 1);
        }

        if (tag.startsWith("sha256:")) {
            repo = remoteName;
            versioning = new Sha256Versioning(tag);
        } else {
            repo = remoteName;
            versioning = new TagVersioning(tag);
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

    @Override
    public String toString() {
        if (versioning == null) {
            return getUnversionedPart();
        } else {
            return getUnversionedPart() + versioning.getSeparator() + versioning.toString();
        }
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
        if (versioning == null) {
            throw new IllegalArgumentException("No image tag was specified in docker image name " +
                "(" + rawName + "). Please provide a tag; this may be 'latest' or a specific version");
        }
        if (!versioning.isValid()) {
            throw new IllegalArgumentException(versioning + " is not a valid image versioning identifier (in " + rawName + ")");
        }
    }

    public String getRegistry() {
        return registry;
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
    private class Sha256Versioning implements Versioning {
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
