package org.testcontainers.utility;

import org.junit.Test;

import static org.junit.Assert.fail;

public class DockerImageNameTest {

    @Test
    public void validNames() {
        testValid("myname:latest");
        testValid("myname:latest");
        testValid("repo/my-name:1.0");
        testValid("repo.foo.com:1234/my-name:1.0");
        testValid("repo.foo.com/my-name:1.0");
        testValid("repo.foo.com:1234/repo_here/my-name:1.0");
        testValid("repo.foo.com:1234/repo-here/my-name@sha256:1234abcd1234abcd1234abcd1234abcd");
        testValid("repo.foo.com:1234/my-name@sha256:1234abcd1234abcd1234abcd1234abcd");
        testValid("1.2.3.4/my-name:1.0");
        testValid("1.2.3.4:1234/my-name:1.0");
        testValid("1.2.3.4/repo-here/my-name:1.0");
        testValid("1.2.3.4:1234/repo-here/my-name:1.0");
    }

    @Test
    public void invalidNames() {
        testInvalid("myname");
        testInvalid(":latest");
        testInvalid("/myname:latest");
        testInvalid("/myname@sha256:latest");
        testInvalid("/myname@sha256:gggggggggggggggggggggggggggggggg");
        testInvalid("repo:notaport/myname:latest");
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
}
