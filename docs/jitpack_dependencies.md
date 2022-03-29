# JitPack (unreleased versions)

If you like to live on the bleeding edge, [jitpack.io](https://jitpack.io) can be used to obtain SNAPSHOT versions.
Use the following dependency description instead:

=== "Maven"
    ```xml
    <dependency>
        <groupId>com.github.testcontainers.testcontainers-java</groupId>
        <artifactId>--artifact name--</artifactId>
        <version>master-SNAPSHOT</version>
    </dependency>
    ```
=== "Gradle"
```groovy
testImplementation "com.github.testcontainers.testcontainers-java:--artifact name--:master-SNAPSHOT"
```

A specific git revision (such as `02782d9`) can be used as a fixed version instead: 

=== "Maven"
    ```xml
    <dependency>
        <groupId>com.github.testcontainers.testcontainers-java</groupId>
        <artifactId>--artifact name--</artifactId>
        <version>02782d9</version>
    </dependency>
    ```
=== "Gradle"
```groovy
testImplementation "com.github.testcontainers.testcontainers-java:--artifact name--:02782d9"
```


The JitPack maven repository must also be declared, e.g.:

=== "Maven"
    ```xml
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>
    ```
=== "Gradle"
    ```groovy
    repositories {
        maven {
            url "https://jitpack.io"
        }
    }
    ```

