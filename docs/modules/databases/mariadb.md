# MariaDB Module

See [Database containers](./index.md) for documentation and usage that is common to all relational database container types.

## MariaDB `root` user password

If no custom password is specified, the container will use the default user password `test` for the `root` user as well.
When you specify a custom password for the database user,
this will also act as the password of the MariaDB `root` user automatically.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:mariadb:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>mariadb</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

!!! hint
    Adding this Testcontainers library JAR will not automatically add a database driver JAR to your project. You should ensure that your project also has a suitable database driver as a dependency.

