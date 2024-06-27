# MySQL Module

See [Database containers](./index.md) for documentation and usage that is common to all relational database container types.

## Overriding MySQL my.cnf settings

For MySQL databases, it is possible to override configuration settings using resources on the classpath. Assuming `somepath/mysql_conf_override`
is a directory on the classpath containing .cnf files, the following URL can be used:

  `jdbc:tc:mysql:8.0.36://hostname/databasename?TC_MY_CNF=somepath/mysql_conf_override`

Any .cnf files in this classpath directory will be mapped into the database container's /etc/mysql/conf.d directory,
and will be able to override server settings when the container starts.

## MySQL `root` user password

If no custom password is specified, the container will use the default user password `test` for the `root` user as well.
When you specify a custom password for the database user,
this will also act as the password of the MySQL `root` user automatically. 

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:mysql:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>mysql</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

!!! hint
    Adding this Testcontainers library JAR will not automatically add a database driver JAR to your project. You should ensure that your project also has a suitable database driver as a dependency.
