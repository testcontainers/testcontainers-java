# Test Containers

[![Circle CI](https://circleci.com/gh/testcontainers/testcontainers-java/tree/master.svg?style=svg)](https://circleci.com/gh/testcontainers/testcontainers-java/tree/master)

Test Containers is a Java library aimed at making it easier to test components or systems that interact with databases and other containerized things. Compared with other approaches, Test Containers is intended to achieve a better balance between compatibility, speed, and overhead of external management.

Test Containers uses Docker to provide lightweight, throwaway instances of real databases, web browsers and web servers for use in your tests. 

You can use TC to obtain a containerized service in one of two ways:

### JUnit @Rule/@ClassRule 

This mode starts a container before your tests and tears it down afterwards. This technique is aimed at JUnit tests that:
 * just need a database temporarily (e.g. DAO tests). _In this sense TC is presented as a possible alternative option to the awesome H2 embedded database_ 
 * need a Chrome or Firefox browser of a fixed version, already wired up to a Selenium RemoteWebDriver and VNC. _In this sense, TC is presented as an alternative to using headless browsers like PhantomJS for selenium tests_
 * need an Nginx web server instance (e.g. for tests that verify app behaviour when run behind a reverse proxy). _This is somewhat experimental and under-developed for now_.

### Containerized database using a specially modified JDBC URL
 
After making a very simple modification to your system's JDBC URL string, Test Containers will provide a disposable stand-in database that can be used without requiring modification to your application code. This is intended to be used for development or integrated testing, when you want consistent, repeatable behaviour without the overhead of managing an external database.

_N.B: TC needs to be on your application's classpath at runtime for this to work_

Examples/Tests:

 * [See here](https://github.com/testcontainers/testcontainers-java/blob/master/modules/mysql/src/test/java/org/rnorth/testcontainers/jdbc/JDBCDriverTest.java)

## Support

Test Containers currently supports:

 * MySQL
 * PostgreSQL
 * Oracle XE
 * nginx
 * the standalone-chrome-debug and standalone-firefox-debug containers from [SeleniumHQ](https://github.com/SeleniumHQ/docker-selenium)
 * generic containers via `GenericContainer` and `GenericContainerRule`

Other container types can be added later. Note that at present, only containers from the Docker Hub registry can be used - this needs to be fixed.

## Usage

### Prerequisites

Docker or boot2docker (for OS X) must be installed on the machine you are running tests on.

### JUnit rule

Add a @Rule or @ClassRule to your test class, e.g.:

    public class SimpleMySQLTest {    
        @Rule
        public MySQLContainerRule mysql = new MySQLContainerRule();

Now, in your test code (or a suitable setup method), you can obtain details necessary to connect to this database:

 * `mysql.getJdbcUrl()` provides a JDBC URL your code can connect to
 * `mysql.getUsername()` provides the username your code should pass to the driver
 * `mysql.getPassword()` provides the password your code should pass to the driver
 
Note that if you use @Rule, you will be given an isolated container for each test method. If you use @ClassRule, you will get on isolated container for all the methods in the test class.

Examples/Tests:

 * [MySQL](https://github.com/testcontainers/testcontainers-java/blob/master/modules/mysql/src/test/java/org/rnorth/testcontainers/junit/SimpleMySQLTest.java)
 * [PostgreSQL](https://github.com/testcontainers/testcontainers-java/blob/master/modules/postgresql/src/test/java/org/rnorth/testcontainers/junit/SimplePostgreSQLTest.java)
 * [nginx](https://github.com/testcontainers/testcontainers-java/blob/master/modules/nginx/src/test/java/org/rnorth/testcontainers/junit/SimpleNginxTest.java)

A generic container rule can be used with any public docker image; for example:

    // Set up a redis container
    @ClassRule
    public static GenericContainerRule redis = new GenericContainerRule("redis:3.0.2")
                                            .withExposedPorts(6379);


    // Set up a plain OS container and customize environment, command and exposed ports. This just listens on port 80 and always returns '42'
    @ClassRule
    public static GenericContainerRule alpine = new GenericContainerRule("alpine:3.2")
                                                   .withExposedPorts(80)
                                                   .withEnv("MAGIC_NUMBER", "42")
                                                   .withCommand("/bin/sh", "-c", "while true; do echo \"$MAGIC_NUMBER\" | nc -l -p 80; done");

### JDBC URL

As long as you have TestContainers and the appropriate JDBC driver on your classpath, you can simply modify regular JDBC connection URLs to get a fresh containerized instance of the database each time your application starts up.

**Original URL**: `jdbc:mysql://somehostname:someport/databasename`

Insert `tc:` after `jdbc:` as follows. Note that the hostname, port and database name will be ignored; you can leave these as-is or set them to any value.

#### Using Test Containers: 

`jdbc:tc:mysql://somehostname:someport/databasename` 

*(Note: this will use the latest version of MySQL)*

#### Using Test Containers with a fixed version: 

`jdbc:tc:mysql:5.6.23://somehostname:someport/databasename`

#### Using PostgreSQL:

`jdbc:tc:postgresql://hostname/databasename`

#### Using an init script 

Test Containers can run an initscript after the database container is started, but before your code is given a connection to it. The script must be on the classpath, and is referenced as follows:

`jdbc:tc:mysql://hostname/databasename?TC_INITSCRIPT=somepath/init_mysql.sql`

This is useful if you have a fixed script for setting up database schema, etc.

#### Using an init function

Instead of running a fixed script for DB setup, it may be useful to call a Java function that you define. This is intended to allow you to trigger database schema migration tools. To do this, add TC_INITFUNCTION to the URL as follows, passing a full path to the class name and method:
 
 `jdbc:tc:mysql://hostname/databasename?TC_INITFUNCTION=org.rnorth.testcontainers.jdbc.JDBCDriverTest::sampleInitFunction`

The init function must be a public static method which takes a `java.sql.Connection` as its only parameter, e.g.

    public class JDBCDriverTest {
        public static void sampleInitFunction(Connection connection) throws SQLException {
            // e.g. run schema setup or Flyway/liquibase/etc DB migrations here...
        }
        ...

## Maven dependency

    <dependency>
        <groupId>org.rnorth.test-containers</groupId>
        <artifactId>test-containers</artifactId>
        <version>0.9.5</version>
    </dependency>

## License

See [LICENSE](LICENSE).

## Roadmap

See [ROADMAP](https://github.com/testcontainers/testcontainers-java/wiki/ROADMAP).

## Attributions

This project includes a modified class (ScriptUtils) taken from the Spring JDBC project, adapted under the terms of the Apache license. Copyright for that class remains with the original authors.

This project is built on top of the awesome [Spotify docker client library for Java](https://github.com/spotify/docker-client) and was initially inspired by a [gist](https://gist.github.com/mosheeshel/c427b43c36b256731a0b) by Mosche Eschel.

## Copyright

Copyright (c) 2015 Richard North
