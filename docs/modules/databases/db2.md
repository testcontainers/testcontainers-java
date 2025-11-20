# DB2 Module

Testcontainers module for [DB2](https://www.ibm.com/docs/en/db2/11.5.x?topic=deployments-db2-community-edition-docker)

## Usage example

You can start a DB2 container instance from any Java application by using:

<!--codeinclude-->
[Container definition](../../../modules/db2/src/test/java/org/testcontainers/db2/Db2ContainerTest.java) inside_block:container
<!--/codeinclude-->

!!! warning "EULA Acceptance"
    Due to licencing restrictions you are required to accept an EULA for this container image. To indicate that you accept the DB2 image EULA, call the `acceptLicense()` method, or place a file at the root of the classpath named `container-license-acceptance.txt`, e.g. at `src/test/resources/container-license-acceptance.txt`. This file should contain the line: `ibmcom/db2:11.5.0.0a` (or, if you are overriding the docker image name/tag, update accordingly).

See [Database containers](./index.md) for documentation and usage that is common to all relational database container types.

### Testcontainers JDBC URL

`jdbc:tc:db2:11.5.0.0a:///databasename`

See [JDBC](./jdbc.md) for documentation.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:testcontainers-db2:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-db2</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

!!! hint
    Adding this Testcontainers library JAR will not automatically add a database driver JAR to your project. You should ensure that your project also has a suitable database driver as a dependency.
