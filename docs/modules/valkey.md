# Valkey

!!! note This module is INCUBATING.
While it is ready for use and operational in the current version of Testcontainers, it is possible that it may receive breaking changes in the future.
See our [contributing guidelines](../contributing.md#incubating-modules) for more information on our incubating modules policy.

Testcontainers module for [Valkey](https://hub.docker.com/r/valkey/valkey)

## Valkey's usage examples

You can start a Valkey container instance from any Java application by using:

<!--codeinclude-->
[Default Valkey container](../../modules/valkey/src/test/java/org/testcontainers/valkey/ValkeyContainerTest.java) inside_block:container
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
```groovy
testImplementation "org.testcontainers:valkey:{{latest_version}}"
```

=== "Maven"
```xml
<dependency>
<groupId>org.testcontainers</groupId>
<artifactId>valkey</artifactId>
<version>{{latest_version}}</version>
<scope>test</scope>
</dependency>
```
