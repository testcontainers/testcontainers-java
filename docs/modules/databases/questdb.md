# QuestDB Module

Testcontainers module for [QuestDB](https://github.com/questdb/questdb). QuestDB is a high-performance, open-source SQL
database for applications in financial services, IoT, machine learning, DevOps and observability.

See [Database containers](./index.md) for documentation and usage that is common to all relational database container
types.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"

```groovy
testImplementation "org.testcontainers:questdb:{{latest_version}}"
```

=== "Maven"

```xml

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>questdb</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
