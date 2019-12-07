# Mockserver Module

Mock Server can be used to mock HTTP services by matching requests against user-defined expectations.

## Usage example

The following example shows how to start Mockserver.

<!--codeinclude-->
[Creating a MockServer container](../../modules/mockserver/src/test/java/org/testcontainers/containers/MockServerContainerTest.java) inside_block:creatingProxy
<!--/codeinclude-->

And how to set a simple expectation using the Java MockServerClient.

<!--codeinclude-->
[Setting a simple expectation](../../modules/mockserver/src/test/java/org/testcontainers/containers/MockServerContainerTest.java) inside_block:testSimpleExpectation
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testCompile "org.testcontainers:mockserver:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mockserver</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```

