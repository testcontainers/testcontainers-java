package org.testcontainers.utility;

import org.junit.Test;

public class DockerImageNameTest {

    @Test
    public void validNames() {
        DockerImageName.validate("myname:latest"); // no repo
        DockerImageName.validate("repo/my-name:1.0"); // no repo
        DockerImageName.validate("repo.foo.com:1234/my-name:1.0"); // no repo
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingTag() {
        DockerImageName.validate("myname");
    }
}