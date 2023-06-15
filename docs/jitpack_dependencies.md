# JitPack (unreleased versions)

If you like to live on the bleeding edge, [jitpack.io](https://jitpack.io) can be used to obtain SNAPSHOT versions.
Use the following dependency description instead:

=== "Gradle"
    ```groovy
    testImplementation "com.github.testcontainers.testcontainers-java:--artifact name--:main-SNAPSHOT"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>com.github.testcontainers.testcontainers-java</groupId>
        <artifactId>--artifact name--</artifactId>
        <version>main-SNAPSHOT</version>
    </dependency>
    ```

A specific git revision (such as `02782d9`) can be used as a fixed version instead: 

=== "Gradle"
    ```groovy
    testImplementation "com.github.testcontainers.testcontainers-java:--artifact name--:02782d9"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>com.github.testcontainers.testcontainers-java</groupId>
        <artifactId>--artifact name--</artifactId>
        <version>02782d9</version>
    </dependency>
    ```


The JitPack maven repository must also be declared, e.g.:

=== "Gradle"
    ```groovy
    repositories {
        maven {
            url "https://jitpack.io"
        }
    }
    ```
=== "Maven"
    ```xml
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>
    ```
