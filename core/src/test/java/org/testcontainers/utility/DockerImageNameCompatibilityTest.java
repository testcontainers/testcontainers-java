package org.testcontainers.utility;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DockerImageNameCompatibilityTest {

    @Test
    public void testPlainImage() {
        DockerImageName subject = DockerImageName.parse("foo");

        assertThat(subject.isCompatibleWith(DockerImageName.parse("bar"))).as("image name foo != bar").isFalse();
    }

    @Test
    public void testNoTagTreatedAsWildcard() {
        final DockerImageName subject = DockerImageName.parse("foo:4.5.6");
        /*
        foo:1.2.3 != foo:4.5.6
        foo:1.2.3 ~= foo

        The test is effectively making sure that 'no tag' is treated as a wildcard
         */
        assertThat(subject.isCompatibleWith(DockerImageName.parse("foo:1.2.3"))).as("foo:4.5.6 != foo:1.2.3").isFalse();
        assertThat(subject.isCompatibleWith(DockerImageName.parse("foo"))).as("foo:4.5.6 ~= foo").isTrue();
    }

    @Test
    public void testImageWithAutomaticCompatibilityForFullPath() {
        DockerImageName subject = DockerImageName.parse("repo/foo:1.2.3");

        assertThat(subject.isCompatibleWith(DockerImageName.parse("repo/foo")))
            .as("repo/foo:1.2.3 ~= repo/foo")
            .isTrue();
    }

    @Test
    public void testImageWithClaimedCompatibility() {
        DockerImageName subject = DockerImageName.parse("foo").asCompatibleSubstituteFor("bar");

        assertThat(subject.isCompatibleWith(DockerImageName.parse("bar"))).as("foo(bar) ~= bar").isTrue();
        assertThat(subject.isCompatibleWith(DockerImageName.parse("fizz"))).as("foo(bar) != fizz").isFalse();
    }

    @Test
    public void testImageWithClaimedCompatibilityAndVersion() {
        DockerImageName subject = DockerImageName.parse("foo:1.2.3").asCompatibleSubstituteFor("bar");

        assertThat(subject.isCompatibleWith(DockerImageName.parse("bar"))).as("foo:1.2.3(bar) ~= bar").isTrue();
    }

    @Test
    public void testImageWithClaimedCompatibilityForFullPath() {
        DockerImageName subject = DockerImageName.parse("foo").asCompatibleSubstituteFor("registry/repo/bar");

        assertThat(subject.isCompatibleWith(DockerImageName.parse("registry/repo/bar")))
            .as("foo(registry/repo/bar) ~= registry/repo/bar")
            .isTrue();
        assertThat(subject.isCompatibleWith(DockerImageName.parse("repo/bar")))
            .as("foo(registry/repo/bar) != repo/bar")
            .isFalse();
        assertThat(subject.isCompatibleWith(DockerImageName.parse("bar")))
            .as("foo(registry/repo/bar) != bar")
            .isFalse();
    }

    @Test
    public void testImageWithClaimedCompatibilityForVersion() {
        DockerImageName subject = DockerImageName.parse("foo").asCompatibleSubstituteFor("bar:1.2.3");

        assertThat(subject.isCompatibleWith(DockerImageName.parse("bar"))).as("foo(bar:1.2.3) ~= bar").isTrue();
        assertThat(subject.isCompatibleWith(DockerImageName.parse("bar:1.2.3")))
            .as("foo(bar:1.2.3) ~= bar:1.2.3")
            .isTrue();
        assertThat(subject.isCompatibleWith(DockerImageName.parse("bar:0.0.1")))
            .as("foo(bar:1.2.3) != bar:0.0.1")
            .isFalse();
        assertThat(subject.isCompatibleWith(DockerImageName.parse("bar:2.0.0")))
            .as("foo(bar:1.2.3) != bar:2.0.0")
            .isFalse();
        assertThat(subject.isCompatibleWith(DockerImageName.parse("bar:1.2.4")))
            .as("foo(bar:1.2.3) != bar:1.2.4")
            .isFalse();
    }

    @Test
    public void testAssertMethodAcceptsCompatible() {
        DockerImageName subject = DockerImageName.parse("foo").asCompatibleSubstituteFor("bar");
        subject.assertCompatibleWith(DockerImageName.parse("bar"));
    }

    @Test
    public void testAssertMethodAcceptsCompatibleLibraryPrefix() {
        DockerImageName subject = DockerImageName.parse("library/foo");
        subject.assertCompatibleWith(DockerImageName.parse("foo"));
    }

    @Test
    public void testAssertMethodRejectsIncompatible() {
        DockerImageName subject = DockerImageName.parse("foo");
        assertThatThrownBy(() -> subject.assertCompatibleWith(DockerImageName.parse("bar")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to verify that image 'foo' is a compatible substitute for 'bar'");
    }
}
