# Database containers

## Benefits

You might want to use Testcontainers' database support:

 * **Instead of H2 database for DAO unit tests that depend on database features that H2 doesn't emulate.** Testcontainers is not as performant as H2, but does give you the benefit of 100% database compatibility (since it runs a real DB inside of a container).
 * **Instead of a database running on the local machine or in a VM** for DAO unit tests or end-to-end integration tests that need a database to be present. In this context, the benefit of Testcontainers is that the database always starts in a known state, without any contamination between test runs or on developers' local machines.

> Note: Of course, it's still important to have as few tests that hit the database as possible, and make good use of mocks for components higher up the stack.

You can obtain a temporary database in one of two ways:

 * **JUnit @Rule/@ClassRule**: this mode starts a database inside a container before your tests and tears it down afterwards.
 * **Using a specially modified JDBC URL**: after making a very simple modification to your system's JDBC URL string, Testcontainers will provide a disposable stand-in database that can be used without requiring modification to your application code.

Testcontainers currently supports MySQL, PostgreSQL, Oracle XE and Virtuoso.

> Note: Oracle XE support does not bundle the proprietary Oracle JDBC drivers - you must provide these yourself.

## Examples and options

### JUnit rule

Add a @Rule or @ClassRule to your test class, e.g.:

```java
public class SimpleMySQLTest {
    @Rule
    public MySQLContainer mysql = new MySQLContainer();
```

Now, in your test code (or a suitable setup method), you can obtain details necessary to connect to this database:

 * `mysql.getJdbcUrl()` provides a JDBC URL your code can connect to
 * `mysql.getUsername()` provides the username your code should pass to the driver
 * `mysql.getPassword()` provides the password your code should pass to the driver

Note that if you use `@Rule`, you will be given an isolated container for each test method. If you use `@ClassRule`, you will get on isolated container for all the methods in the test class.

Examples/Tests:

 * [MySQL](https://github.com/testcontainers/testcontainers-java/blob/master/modules/jdbc-test/src/test/java/org/testcontainers/junit/SimpleMySQLTest.java)
 * [PostgreSQL](https://github.com/testcontainers/testcontainers-java/blob/master/modules/jdbc-test/src/test/java/org/testcontainers/junit/SimplePostgreSQLTest.java)
 * [Oracle-XE](https://github.com/testcontainers/testcontainers-java-module-oracle-xe/blob/master/src/test/java/org/testcontainers/junit/SimpleOracleTest.java)
 * [Virtuoso](https://github.com/testcontainers/testcontainers-java/blob/master/modules/virtuoso/src/test/java/org/testcontainers/junit/SimpleVirtuosoTest.java)

### JDBC URL

As long as you have Testcontainers and the appropriate JDBC driver on your classpath, you can simply modify regular JDBC connection URLs to get a fresh containerized instance of the database each time your application starts up.

_N.B:_
* _TC needs to be on your application's classpath at runtime for this to work_
* _For Spring Boot you need to specify the driver manually `spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver`_

**Original URL**: `jdbc:mysql:5.7.22://somehostname:someport/databasename`

Insert `tc:` after `jdbc:` as follows. Note that the hostname, port and database name will be ignored; you can leave these as-is or set them to any value.

### JDBC URL examples

#### DEPRECATED: Simple Testcontainers JDBC driver usage

`jdbc:tc:mysql://somehostname:someport/databasename`

*(Note: this will use a fixed version of the database. You should typically specify the version you desire via a tag parameter, as below).*

#### Using Testcontainers with a fixed version

`jdbc:tc:mysql:5.6.23://somehostname:someport/databasename`

#### Using PostgreSQL

`jdbc:tc:postgresql:9.6.8://hostname/databasename`


## Using an init script

Testcontainers can run an initscript after the database container is started, but before your code is given a connection to it. The script must be on the classpath, and is referenced as follows:

`jdbc:tc:mysql:5.7.22://hostname/databasename?TC_INITSCRIPT=somepath/init_mysql.sql`

This is useful if you have a fixed script for setting up database schema, etc.

#### Using an init function

Instead of running a fixed script for DB setup, it may be useful to call a Java function that you define. This is intended to allow you to trigger database schema migration tools. To do this, add TC_INITFUNCTION to the URL as follows, passing a full path to the class name and method:

 `jdbc:tc:mysql:5.7.22://hostname/databasename?TC_INITFUNCTION=org.testcontainers.jdbc.JDBCDriverTest::sampleInitFunction`

The init function must be a public static method which takes a `java.sql.Connection` as its only parameter, e.g.
```java
public class JDBCDriverTest {
    public static void sampleInitFunction(Connection connection) throws SQLException {
        // e.g. run schema setup or Flyway/liquibase/etc DB migrations here...
    }
    ...
```

#### Running container in daemon mode

By default database container is being stopped as soon as last connection is closed. There are cases when you might need to start container and keep it running till you stop it explicitly or JVM is shutdown. To do this, add `TC_DAEMON` parameter to the URL as follows:

 `jdbc:tc:mysql:5.7.22://hostname/databasename?TC_DAEMON=true`

With this parameter database container will keep running even when there're no open connections.

#### Overriding MySQL my.cnf settings

For MySQL databases, it is possible to override configuration settings using resources on the classpath. Assuming `somepath/mysql_conf_override`
is a directory on the classpath containing .cnf files, the following URL can be used:

  `jdbc:tc:mysql:5.6://hostname/databasename?TC_MY_CNF=somepath/mysql_conf_override`

Any .cnf files in this classpath directory will be mapped into the database container's /etc/mysql/conf.d directory,
and will be able to override server settings when the container starts.
