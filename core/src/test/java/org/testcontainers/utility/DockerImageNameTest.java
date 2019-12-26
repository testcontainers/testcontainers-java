package org.testcontainers.utility;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.rnorth.visibleassertions.VisibleAssertions;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

@RunWith(Enclosed.class)
public class DockerImageNameTest {

    @RunWith(Parameterized.class)
    public static class ValidNames {

        @Parameterized.Parameters(name = "{0}")
        public static String[] getNames() {
            return new String[]{
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
                "1.2.3.4:1234/repo-here/my-name:1.0"};
        }

        @Parameterized.Parameter
        public String imageName;

        @Test
        public void testValidNameAccepted() {
            new DockerImageName(imageName).assertValid();
        }
    }

    @RunWith(Parameterized.class)
    public static class InvalidNames {

        @Parameterized.Parameters(name = "{0}")
        public static String[] getNames() {
            return new String[]{":latest",
                "/myname:latest",
                "/myname@sha256:latest",
                "/myname@sha256:gggggggggggggggggggggggggggggggg",
                "repo:notaport/myname:latest"};
        }

        @Parameterized.Parameter
        public String imageName;

        @Test(expected = IllegalArgumentException.class)
        public void testInvalidNameRejected() {
            new DockerImageName(imageName).assertValid();
        }
    }

    @RunWith(Parameterized.class)
    public static class Parsing {

        @Parameterized.Parameters(name = "{0}{1}{2}{3}{4}")
        public static String[][] getNames() {
            return new String[][]{
                {"", "", "myname", ":", "latest"},
                {"", "", "repo/myname", ":", "latest"},
                {"registry.foo.com:1234", "/", "my-name", ":", "1.0"},
                {"registry.foo.com", "/", "my-name", ":", "1.0"},
                {"registry.foo.com:1234", "/", "repo_here/my-name", ":", "1.0"},
                {"1.2.3.4:1234", "/", "repo_here/my-name", ":", "1.0"},
                {"1.2.3.4:1234", "/", "my-name", ":", "1.0"},
                {"", "", "myname", "@", "sha256:1234abcd1234abcd1234abcd1234abcd"},
                {"", "", "repo/myname", "@", "sha256:1234abcd1234abcd1234abcd1234abcd"},
                {"registry.foo.com:1234", "/", "repo-here/my-name", "@", "sha256:1234abcd1234abcd1234abcd1234abcd"},
                {"registry.foo.com:1234", "/", "my-name", "@", "sha256:1234abcd1234abcd1234abcd1234abcd"},
                {"1.2.3.4", "/", "my-name", "@", "sha256:1234abcd1234abcd1234abcd1234abcd"},
                {"1.2.3.4:1234", "/", "my-name", "@", "sha256:1234abcd1234abcd1234abcd1234abcd"},
                {"1.2.3.4", "/", "my-name", "@", "sha256:1234abcd1234abcd1234abcd1234abcd"},
                {"1.2.3.4:1234", "/", "my-name", "@", "sha256:1234abcd1234abcd1234abcd1234abcd"}
            };
        }

        @Parameterized.Parameter(0)
        public String registry;
        @Parameterized.Parameter(1)
        public String registrySeparator;
        @Parameterized.Parameter(2)
        public String repo;
        @Parameterized.Parameter(3)
        public String versionSeparator;
        @Parameterized.Parameter(4)
        public String version;

        @Test
        public void testParsing() {
            final String unversionedPart = registry + registrySeparator + repo;
            final String combined = unversionedPart + versionSeparator + version;

            VisibleAssertions.context("For " + combined);
            VisibleAssertions.context("Using single-arg constructor:", 2);

            final DockerImageName imageName = new DockerImageName(combined);
            assertEquals(combined + " has registry address: " + registry, registry, imageName.getRegistry());
            assertEquals(combined + " has unversioned part: " + unversionedPart, unversionedPart, imageName.getUnversionedPart());
            assertEquals(combined + " has version part: " + version, version, imageName.getVersionPart());

            VisibleAssertions.context("Using two-arg constructor:", 2);

            final DockerImageName imageNameFromSecondaryConstructor = new DockerImageName(unversionedPart, version);
            assertEquals(combined + " has registry address: " + registry, registry, imageNameFromSecondaryConstructor.getRegistry());
            assertEquals(combined + " has unversioned part: " + unversionedPart, unversionedPart, imageNameFromSecondaryConstructor.getUnversionedPart());
            assertEquals(combined + " has version part: " + version, version, imageNameFromSecondaryConstructor.getVersionPart());
        }
    }
}
