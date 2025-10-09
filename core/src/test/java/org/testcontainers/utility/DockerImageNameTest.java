package org.testcontainers.utility;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DockerImageNameTest {

    @Nested
    class ValidNames {

        public static String[] getNames() {
            return new String[] {
                "myname:latest",
                "repo/my-name:1.0",
                "registry.foo.com:1234/my-name:1.0",
                "registry.foo.com/my-name:1.0",
                "registry.foo.com:1234/repo_here/my-name:1.0",
                "registry.foo.com:1234/repo-here/my-name@sha256:1234abcd1234abcd1234abcd1234abcd",
                "registry.foo.com:1234/my-name@sha256:1234abcd1234abcd1234abcd1234abcd",
                "1.2.3.4/my-name:1.0",
                "1.2.3.4:1234/my-name:1.0",
                "1.2.3.4/repo-here/my-name:1.0",
                "1.2.3.4:1234/repo-here/my-name:1.0",
            };
        }

        @ParameterizedTest
        @MethodSource("getNames")
        void testValidNameAccepted(String imageName) {
            DockerImageName.parse(imageName).assertValid();
        }
    }

    @Nested
    class InvalidNames {

        public static String[] getNames() {
            return new String[] {
                ":latest",
                "/myname:latest",
                "/myname@sha256:latest",
                "/myname@sha256:gggggggggggggggggggggggggggggggg",
                "repo:notaport/myname:latest",
            };
        }

        @ParameterizedTest
        @MethodSource("getNames")
        void testInvalidNameRejected(String imageName) {
            assertThatThrownBy(() -> DockerImageName.parse(imageName).assertValid())
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Parsing {

        public static Stream<Arguments> getNames() {
            return Stream.of(
                Arguments.of("", "", "myname", ":", null),
                Arguments.of("", "", "myname", ":", "latest"),
                Arguments.of("", "", "repo/myname", ":", null),
                Arguments.of("", "", "repo/myname", ":", "latest"),
                Arguments.of("registry.foo.com:1234", "/", "my-name", ":", null),
                Arguments.of("registry.foo.com:1234", "/", "my-name", ":", "1.0"),
                Arguments.of("registry.foo.com", "/", "my-name", ":", "1.0"),
                Arguments.of("registry.foo.com:1234", "/", "repo_here/my-name", ":", null),
                Arguments.of("registry.foo.com:1234", "/", "repo_here/my-name", ":", "1.0"),
                Arguments.of("1.2.3.4:1234", "/", "repo_here/my-name", ":", null),
                Arguments.of("1.2.3.4:1234", "/", "repo_here/my-name", ":", "1.0"),
                Arguments.of("1.2.3.4:1234", "/", "my-name", ":", null),
                Arguments.of("1.2.3.4:1234", "/", "my-name", ":", "1.0"),
                Arguments.of("", "", "myname", "@", "sha256:1234abcd1234abcd1234abcd1234abcd"),
                Arguments.of("", "", "repo/myname", "@", "sha256:1234abcd1234abcd1234abcd1234abcd"),
                Arguments.of(
                    "registry.foo.com:1234",
                    "/",
                    "repo-here/my-name",
                    "@",
                    "sha256:1234abcd1234abcd1234abcd1234abcd"
                ),
                Arguments.of("registry.foo.com:1234", "/", "my-name", "@", "sha256:1234abcd1234abcd1234abcd1234abcd"),
                Arguments.of("1.2.3.4", "/", "my-name", "@", "sha256:1234abcd1234abcd1234abcd1234abcd"),
                Arguments.of("1.2.3.4:1234", "/", "my-name", "@", "sha256:1234abcd1234abcd1234abcd1234abcd"),
                Arguments.of("1.2.3.4", "/", "my-name", "@", "sha256:1234abcd1234abcd1234abcd1234abcd"),
                Arguments.of("1.2.3.4:1234", "/", "my-name", "@", "sha256:1234abcd1234abcd1234abcd1234abcd")
            );
        }

        @ParameterizedTest
        @MethodSource("getNames")
        void testParsing(
            String registry,
            String registrySeparator,
            String repo,
            String versionSeparator,
            String version
        ) {
            final String unversionedPart = registry + registrySeparator + repo;

            String combined;
            String canonicalName;
            if (version != null) {
                combined = unversionedPart + versionSeparator + version;
                canonicalName = unversionedPart + versionSeparator + version;
            } else {
                combined = unversionedPart;
                canonicalName = unversionedPart + ":latest";
            }

            final DockerImageName imageName = DockerImageName.parse(combined);
            assertThat(imageName.getRegistry()).as(combined + " has registry address: " + registry).isEqualTo(registry);
            assertThat(imageName.getUnversionedPart())
                .as(combined + " has unversioned part: " + unversionedPart)
                .isEqualTo(unversionedPart);
            if (version != null) {
                assertThat(imageName.getVersionPart())
                    .as(combined + " has version part: " + version)
                    .isEqualTo(version);
            } else {
                assertThat(imageName.getVersionPart())
                    .as(combined + " has automatic 'latest' version specified")
                    .isEqualTo("latest");
            }
            assertThat(imageName.asCanonicalNameString())
                .as(combined + " has canonical name: " + canonicalName)
                .isEqualTo(canonicalName);

            if (version != null) {
                final DockerImageName imageNameFromSecondaryConstructor = new DockerImageName(unversionedPart, version);
                assertThat(imageNameFromSecondaryConstructor.getRegistry())
                    .as(combined + " has registry address: " + registry)
                    .isEqualTo(registry);
                assertThat(imageNameFromSecondaryConstructor.getUnversionedPart())
                    .as(combined + " has unversioned part: " + unversionedPart)
                    .isEqualTo(unversionedPart);
                assertThat(imageNameFromSecondaryConstructor.getVersionPart())
                    .as(combined + " has version part: " + version)
                    .isEqualTo(version);
            }
        }
    }
}
