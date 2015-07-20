# TestContainers

> TestContainers is a Java library aimed at making it easier to test components or systems that interact with databases and other containerized things. TestContainers uses Docker to provide lightweight, throwaway instances of your tests' dependencies.

[![Circle CI](https://circleci.com/gh/testcontainers/testcontainers-java/tree/master.svg?style=svg)](https://circleci.com/gh/testcontainers/testcontainers-java/tree/master) 
[![Gitter chat](https://img.shields.io/badge/gitter-chat-blue.svg)](https://gitter.im/testcontainers/general)


## Table of Contents
<!-- MarkdownTOC autolink=true bracket=round depth=3 -->

- [Use Cases](#use-cases)
- [Usage summary](#usage-summary)
- [Usage](#usage)
    - [Prerequisites](#prerequisites)
    - [JUnit rule](#junit-rule)
    - [JDBC URL](#jdbc-url)
- [Maven dependency](#maven-dependency)
- [Supported containers](#supported-containers)
- [License](#license)
- [Attributions](#attributions)
- [Contributing](#contributing)
- [Copyright](#copyright)

<!-- /MarkdownTOC -->

## Use Cases

 * **Data access layer integration tests**: use a containerized instance of a MySQL, PostgreSQL or Oracle database to test your data access layer code for complete compatibility, but without requiring complex setup on developers' machines and safe in the knowledge that your tests will always start with a known DB state. Any other database type that can be containerized can also be used.
 * **Application integration tests**: for running your application in a short-lived test mode with dependencies, such as databases, message queues or web servers.
 * **UI/Acceptance tests**: use containerized web browsers, compatible with Selenium, for conducting automated UI tests. Each test can get a fresh instance of the browser, with no browser state, plugin variations or automated browser upgrades to worry about. And you get a video recording of each test session, or just each session where tests failed.

## Usage summary

You can use TC to obtain a containerized service in one of two ways:

 * **JUnit @Rule/@ClassRule**: this mode starts a container before your tests and tears it down afterwards.
 * **Containerized database using a specially modified JDBC URL**: after making a very simple modification to your system's JDBC URL string, TestContainers will provide a disposable stand-in database that can be used without requiring modification to your application code.

## Usage

### Prerequisites

Docker or boot2docker (for OS X) must be installed on the machine you are running tests on. TestContainers currently requires JDK 1.7 and is compatible with JUnit and Selenium2/WebDriver.

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

 * [MySQL](https://github.com/testcontainers/testcontainers-java/blob/master/modules/mysql/src/test/java/org/testcontainers/junit/SimpleMySQLTest.java)
 * [PostgreSQL](https://github.com/testcontainers/testcontainers-java/blob/master/modules/postgresql/src/test/java/org/testcontainers/junit/SimplePostgreSQLTest.java)
 * [nginx](https://github.com/testcontainers/testcontainers-java/blob/master/modules/nginx/src/test/java/org/testcontainers/junit/SimpleNginxTest.java)

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

_N.B: TC needs to be on your application's classpath at runtime for this to work_

**Original URL**: `jdbc:mysql://somehostname:someport/databasename`

Insert `tc:` after `jdbc:` as follows. Note that the hostname, port and database name will be ignored; you can leave these as-is or set them to any value.

#### Examples

##### Simple TestContainers JDBC driver usage

`jdbc:tc:mysql://somehostname:someport/databasename`

*(Note: this will use the latest version of MySQL)*

##### Using TestContainers with a fixed version

`jdbc:tc:mysql:5.6.23://somehostname:someport/databasename`

##### Using PostgreSQL

`jdbc:tc:postgresql://hostname/databasename`

##### Using an init script

TestContainers can run an initscript after the database container is started, but before your code is given a connection to it. The script must be on the classpath, and is referenced as follows:

`jdbc:tc:mysql://hostname/databasename?TC_INITSCRIPT=somepath/init_mysql.sql`

This is useful if you have a fixed script for setting up database schema, etc.

##### Using an init function

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

## Supported containers

TestContainers currently supports:

 * MySQL
 * PostgreSQL
 * Oracle XE
 * nginx
 * the standalone-chrome-debug and standalone-firefox-debug containers from [SeleniumHQ](https://github.com/SeleniumHQ/docker-selenium)
 * any other container images using `GenericContainer` and `GenericContainerRule`

Other container types can be added later. Note that at present, only containers from the Docker Hub registry can be used - this needs to be fixed.

## License

See [LICENSE](LICENSE).

## Attributions

This project includes a modified class (ScriptUtils) taken from the Spring JDBC project, adapted under the terms of the Apache license. Copyright for that class remains with the original authors.

This project is built on top of the awesome [Spotify docker client library for Java](https://github.com/spotify/docker-client) and was initially inspired by a [gist](https://gist.github.com/mosheeshel/c427b43c36b256731a0b) by Mosche Eschel.

## Contributing

* Star the project on Github and help spread the word :)
* See [ROADMAP](https://github.com/testcontainers/testcontainers-java/wiki/ROADMAP) to understand the approach behind the project and what may/may not be in store for the future.
* [Post an issue](https://github.com/testcontainers/testcontainers-java/issues) if you find any bugs
* Contribute improvements or fixes using a [Pull Request](https://github.com/testcontainers/testcontainers-java/pulls). If you're going to contribute, thank you! Please just be sure to:
	* discuss with the authors on an issue ticket prior to doing anything big
	* follow the style, naming and structure conventions of the rest of the project
	* make commits atomic and easy to merge
	* verify all tests are passing. Build the project with `mvn clean install -Pproprietary-deps` to do this.

## Copyright

Copyright (c) 2015 Richard North and other authors.

See [AUTHORS](AUTHORS) for contributors.
