# MariaDB Module

Testcontainers module for [MariaDB](https://hub.docker.com/_/mariadb)

## Usage example

You can start a MySQL container instance from any Java application by using:

<!--codeinclude-->
[Container definition](../../../modules/mariadb/src/test/java/org/testcontainers/mariadb/MariaDBContainerTest.java) inside_block:container
<!--/codeinclude-->

See [Database containers](./index.md) for documentation and usage that is common to all relational database container types.

### Testcontainers JDBC URL

`jdbc:tc:mariadb:10.3.39:///databasename`

See [JDBC](./jdbc.md) for documentation.

## MariaDB `root` user password

If no custom password is specified, the container will use the default user password `test` for the `root` user as well.
When you specify a custom password for the database user,
this will also act as the password of the MariaDB `root` user automatically.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:testcontainers-mariadb:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-mariadb</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

!!! hint
    Adding this Testcontainers library JAR will not automatically add a database driver JAR to your project. You should ensure that your project also has a suitable database driver as a dependency.

