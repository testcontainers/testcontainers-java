# Nginx Module

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>nginx</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```

```groovy tab='Gradle'
testRuntime "org.testcontainers:nginx:{{latest_version}}"
```
