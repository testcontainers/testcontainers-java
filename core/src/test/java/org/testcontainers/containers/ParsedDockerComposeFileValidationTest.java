package org.testcontainers.containers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import static java.util.Collections.*;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

public class ParsedDockerComposeFileValidationTest {

    @Test
    public void shouldValidate() {
        File file = new File("src/test/resources/docker-compose-container-name-v1.yml");
        Assertions
            .assertThatThrownBy(() -> {
                new ParsedDockerComposeFile(file, emptyMap());
            })
            .hasMessageContaining(file.getAbsolutePath())
            .hasMessageContaining("'container_name' property set for service 'redis'");
    }

    @Test
    public void shouldRejectContainerNameV1() {
        Assertions
            .assertThatThrownBy(() -> {
                new ParsedDockerComposeFile(ImmutableMap.of(
                    "redis", ImmutableMap.of("container_name", "redis")
                ), emptyMap());
            })
            .hasMessageContaining("'container_name' property set for service 'redis'");
    }

    @Test
    public void shouldRejectContainerNameV2() {
        Assertions
            .assertThatThrownBy(() -> {
                new ParsedDockerComposeFile(ImmutableMap.of(
                    "version", "2",
                    "services", ImmutableMap.of(
                        "redis", ImmutableMap.of("container_name", "redis")
                    )
                ), emptyMap());
            })
            .hasMessageContaining("'container_name' property set for service 'redis'");
    }

    @Test
    public void shouldIgnoreUnknownStructure() {
        // Everything is a list
        new ParsedDockerComposeFile(emptyMap(), emptyMap());

        // services is not a map but List
        new ParsedDockerComposeFile(ImmutableMap.of(
            "version", "2",
            "services", emptyList()
        ), Collections.emptyMap());

        // services is not a collection
        new ParsedDockerComposeFile(ImmutableMap.of(
            "version", "2",
            "services", true
        ), Collections.emptyMap());

        // no services while version is defined
        new ParsedDockerComposeFile(ImmutableMap.of(
            "version", "9000"
        ), Collections.emptyMap());
    }

    @Test
    public void shouldObtainImageNamesV1() {
        File file = new File("src/test/resources/docker-compose-imagename-parsing-v1.yml");
        ParsedDockerComposeFile parsedFile = new ParsedDockerComposeFile(file, emptyMap());
        assertEquals("all defined images are found", Sets.newHashSet("redis", "mysql", "postgres"), parsedFile.getDependencyImageNames()); // redis, mysql from compose file, postgres from Dockerfile build
    }

    @Test
    public void shouldObtainImageNamesV2() {
        File file = new File("src/test/resources/docker-compose-imagename-parsing-v2.yml");
        ParsedDockerComposeFile parsedFile = new ParsedDockerComposeFile(file, emptyMap());
        assertEquals("all defined images are found", Sets.newHashSet("redis", "mysql", "postgres"), parsedFile.getDependencyImageNames()); // redis, mysql from compose file, postgres from Dockerfile build
    }

    @Test
    public void shouldObtainImageFromDockerfileBuild() {
        File file = new File("src/test/resources/docker-compose-imagename-parsing-dockerfile.yml");
        ParsedDockerComposeFile parsedFile = new ParsedDockerComposeFile(file, emptyMap());
        assertEquals("all defined images are found", Sets.newHashSet("redis", "mysql", "alpine:3.2"), parsedFile.getDependencyImageNames()); // redis, mysql from compose file, alpine:3.2 from Dockerfile build
    }

    @Test
    public void shouldObtainImageFromDockerfileBuildWithContext() {
        File file = new File("src/test/resources/docker-compose-imagename-parsing-dockerfile-with-context.yml");
        ParsedDockerComposeFile parsedFile = new ParsedDockerComposeFile(file, emptyMap());
        assertEquals("all defined images are found", Sets.newHashSet("redis", "mysql", "alpine:3.2"), parsedFile.getDependencyImageNames()); // redis, mysql from compose file, alpine:3.2 from Dockerfile build
    }

    @Test
    public void foo() {
        File file = new File("src/test/resources/docker-compose-imagename-parsing-env-substitution.yml");

        HashMap<String, String> env = new HashMap<>();
        env.put("DEFINED_A", "A");
        env.put("DEFINED_D", "D");
        env.put("DEFINED_G", "G");
        env.put("DEFINED_J", "J");
        env.put("DEFINED_M", "M");
        env.put("EMPTY", "");

        HashSet<String> expected = Sets.newHashSet(
            "a-prefix-A-suffix",
            "b-prefix--suffix",
            "c-prefix--suffix",
            "d-prefix-D-suffix",
            "e-prefix-e-default-suffix",
            "f-prefix--suffix",
            "g-prefix-G-suffix",
            "h-prefix-h-default-suffix",
            "i-prefix-i-default-suffix",
            "j-prefix-J-suffix",
            "k-prefix--suffix",
            "l-prefix--suffix",
            "m-prefix-M-suffix",
            "n-prefix--suffix",
            "o-prefix--suffix"
        );

        ParsedDockerComposeFile parsedFile = new ParsedDockerComposeFile(file, env);
        assertEquals("all defined images are found", expected, parsedFile.getDependencyImageNames()); // redis, mysql from compose file, alpine:3.2 from Dockerfile build
    }
}
