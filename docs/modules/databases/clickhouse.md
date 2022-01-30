# Clickhouse Module

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testImplementation "org.testcontainers:clickhouse:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>clickhouse</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```

!!! hint
    Adding this Testcontainers library JAR will not automatically add a database driver JAR to your project. You should ensure that your project also has a suitable database driver as a dependency.

!!! note
    Testcontainers uses the new ClickHouse driver (`com.clickhouse.jdbc.ClickHouseDriver`) by default after version 1.16.3, but the new driver only supports ClickHouse with version >= 20.7.
    
    For compatibility with ClickHouse versions < 20.7, you can temporarily continue to use the old ClickHouse driver(`ru.yandex.clickhouse.ClickHouseDriver`) by adding the following JVM config option: `-Dclickhouse-temporarily-use-deprecated-driver=true`
    
    Future versions of Testcontainers will not support the old driver after July 2022 and it is recommended that you to use ClickHouse version 20.7 or above.

