# ActiveMQ

Testcontainers module for [ActiveMQ](https://hub.docker.com/r/apache/activemq-classic) and
[Artemis](https://hub.docker.com/r/apache/activemq-artemis).

## ActiveMQContainer's usage examples

<!--codeinclude-->
[Default ActiveMQ container](../../modules/activemq/src/test/java/org/testcontainers/activemq/ActiveMQContainerTest.java) inside_block:container
<!--/codeinclude-->

## ArtemisContainer's usage examples

<!--codeinclude-->
[Default Artemis container](../../modules/activemq/src/test/java/org/testcontainers/activemq/ArtemisContainerTest.java) inside_block:container
<!--/codeinclude-->

<!--codeinclude-->
[Setting custom credentials](../../modules/activemq/src/test/java/org/testcontainers/activemq/ArtemisContainerTest.java) inside_block:settingCredentials
<!--/codeinclude-->

<!--codeinclude-->
[Allow anonymous login](../../modules/activemq/src/test/java/org/testcontainers/activemq/ArtemisContainerTest.java) inside_block:anonymousLogin
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:activemq:{{latest_version}}"
    ```

=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>activemq</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
