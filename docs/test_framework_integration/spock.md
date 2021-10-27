# Spock

[Spock](https://github.com/spockframework/spock) extension for [Testcontainers](https://github.com/testcontainers/testcontainers-java) library, which allows to use Docker containers inside of Spock tests.

## Usage

### `@Testcontainers` class-annotation

Specifying the `@Testcontainers` annotation will instruct Spock to start and stop all testcontainers accordingly. This annotation 
can be mixed with Spock's `@Shared` annotation to indicate, that containers shouldn't be restarted between tests.

<!--codeinclude-->
[PostgresContainerIT](../../modules/spock/src/test/groovy/org/testcontainers/spock/PostgresContainerIT.groovy) inside_block:PostgresContainerIT
<!--/codeinclude-->

## Adding Testcontainers Spock support to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testImplementation "org.testcontainers:spock:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>spock</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```


## Attributions
The initial version of this project was heavily inspired by the excellent [JUnit5 docker extension](https://github.com/FaustXVI/junit5-docker) by [FaustXVI](https://github.com/FaustXVI).
