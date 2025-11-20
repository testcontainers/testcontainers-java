# QuestDB Module

Testcontainers module for [QuestDB](https://hub.docker.com/r/questdb/questdb)

## Usage example

You can start a QuestDB container instance from any Java application by using:

<!--codeinclude-->
[Container creation](../../../modules/questdb/src/test/java/org/testcontainers/junit/questdb/SimpleQuestDBTest.java) inside_block:container
<!--/codeinclude-->

See [Database containers](./index.md) for documentation and usage that is common to all relational database container
types.

### Testcontainers JDBC URL

`jdbc:tc:questdb:6.5.3:///databasename`

See [JDBC](./jdbc.md) for documentation.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"

```groovy
testImplementation "org.testcontainers:testcontainers-questdb:{{latest_version}}"
```

=== "Maven"

```xml

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-questdb</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
