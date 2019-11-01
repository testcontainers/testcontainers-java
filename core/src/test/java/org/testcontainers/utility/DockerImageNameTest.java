package org.testcontainers.utility;

import com.google.common.base.Strings;
import org.junit.Test;
import org.rnorth.visibleassertions.VisibleAssertions;

import static org.junit.Assert.fail;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

public class DockerImageNameTest {

    @Test
    public void validNames() {
        testValid("myname:latest");
        testValid("myname:latest");
        testValid("repo/my-name:1.0");
        testValid("registry.foo.com:1234/my-name:1.0");
        testValid("registry.foo.com/my-name:1.0");
        testValid("registry.foo.com:1234/repo_here/my-name:1.0");
        testValid("registry.foo.com:1234/repo-here/my-name@sha256:1234abcd1234abcd1234abcd1234abcd");
        testValid("registry.foo.com:1234/my-name@sha256:1234abcd1234abcd1234abcd1234abcd");
        testValid("1.2.3.4/my-name:1.0");
        testValid("1.2.3.4:1234/my-name:1.0");
        testValid("1.2.3.4/repo-here/my-name:1.0");
        testValid("1.2.3.4:1234/repo-here/my-name:1.0");
    }

    @Test
    public void invalidNames() {
        testInvalid(":latest");
        testInvalid("/myname:latest");
        testInvalid("/myname@sha256:latest");
        testInvalid("/myname@sha256:gggggggggggggggggggggggggggggggg");
        testInvalid("repo:notaport/myname:latest");
    }

    @Test
    public void parsingForSimpleTagVersions() {
        testParsing("", "myname", ":", "latest");
        testParsing("", "repo/myname", ":", "latest");
        testParsing("registry.foo.com:1234", "my-name", ":", "1.0");
        testParsing("registry.foo.com", "my-name", ":", "1.0");
        testParsing("registry.foo.com:1234", "repo_here/my-name", ":", "1.0");
        testParsing("1.2.3.4:1234", "repo_here/my-name", ":", "1.0");
        testParsing("1.2.3.4:1234", "my-name", ":", "1.0");
    }

    @Test
    public void parsingForShaSumVersions() {
        testParsing("registry.foo.com:1234", "repo-here/my-name", "@", "sha256:1234abcd1234abcd1234abcd1234abcd");
        testParsing("registry.foo.com:1234", "my-name", "@", "sha256:1234abcd1234abcd1234abcd1234abcd");
        testParsing("1.2.3.4", "my-name", "@", "sha256:1234abcd1234abcd1234abcd1234abcd");
        testParsing("1.2.3.4:1234", "my-name", "@", "sha256:1234abcd1234abcd1234abcd1234abcd");
        testParsing("1.2.3.4", "my-name", "@", "sha256:1234abcd1234abcd1234abcd1234abcd");
        testParsing("1.2.3.4:1234", "my-name", "@", "sha256:1234abcd1234abcd1234abcd1234abcd");
    }

    private void testValid(String s) {
        new DockerImageName(s).assertValid();
    }

    private void testInvalid(String myname) {
        try {
            new DockerImageName(myname).assertValid();
            fail();
        } catch (IllegalArgumentException expected) {

        }
    }

    private void testParsing(final String registry, final String repo, final String versionSeparator, final String version) {

        String unversionedPart;
        if (Strings.isNullOrEmpty(registry)) {
            unversionedPart = repo;
        } else {
            unversionedPart = registry + "/" + repo;
        }

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
