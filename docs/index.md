# TestContainers

> TestContainers is a Java library that supports JUnit tests, providing lightweight, throwaway instances of common databases, Selenium web browsers, or anything else that can run in a Docker container.

[![Circle CI](https://circleci.com/gh/testcontainers/testcontainers-java/tree/master.svg?style=svg)](https://circleci.com/gh/testcontainers/testcontainers-java/tree/master) [View on Github](https://github.com/testcontainers/testcontainers-java)

![Testcontainers logo](https://rnorth.org/public/testcontainers/logo.png)

## Use Cases

TestContainers makes it easy to launch useful Docker containers for the duration of JUnit tests.

 * **Data access layer integration tests**: use a containerized instance of a MySQL, PostgreSQL or Oracle database to test your data access layer code for complete compatibility, but without requiring complex setup on developers' machines and safe in the knowledge that your tests will always start with a known DB state. Any other database type that can be containerized can also be used.
 * **Application integration tests**: for running your application in a short-lived test mode with dependencies, such as databases, message queues or web servers.
 * **UI/Acceptance tests**: use containerized web browsers, compatible with Selenium, for conducting automated UI tests. Each test can get a fresh instance of the browser, with no browser state, plugin variations or automated browser upgrades to worry about. And you get a video recording of each test session, or just each session where tests failed.

## Usage

### Prerequisites

Docker or docker-machine (for OS X) must be installed on the machine you are running tests on. TestContainers currently requires JDK 1.8 and is compatible with JUnit.

Note that only Linux host environments, or OS X with Docker Machine, are currently supported.

If you want to use TestContainers on Windows you can try the [alpha release](usage/windows_support.md).

### Docker environment discovery

Testcontainers will try to connect to a Docker daemon using the following strategies in order:

* Environment variables:
	* `DOCKER_HOST` (this should be set to an HTTP/HTTPS connection rather than a unix socket at present)
	* `DOCKER_TLS_VERIFY`
	* `DOCKER_CERT_PATH`
* Defaults:
	* `DOCKER_HOST=https://localhost:2376`
	* `DOCKER_TLS_VERIFY=1`
	* `DOCKER_CERT_PATH=~/.docker`
* If Docker Machine is installed, the docker machine environment for the *first* machine found. Docker Machine needs to be on the PATH for this to succeed.

### Usage modes

* [Temporary database containers](usage/database_containers.md) - specialized MySQL, PostgreSQL, Oracle XE and Virtuoso container support
* [Webdriver containers](usage/webdriver_containers.md) - run a Dockerized Chrome or Firefox browser ready for Selenium/Webdriver operations - complete with automatic video recording
* [Generic containers](usage/generic_containers.md) - run any Docker container as a test dependency
* [Docker compose](usage/docker_compose.md) - reuse services defined in a Docker Compose YAML file
* [Dockerfile containers](usage/dockerfile.md) - run a container that is built on-the-fly from a Dockerfile

## Maven dependencies

TestContainers is distributed in a handful of Maven modules:

* **testcontainers** for just core functionality, generic containers and docker-compose support
* **mysql**, **postgresql** or **oracle-xe** for database container support
* **selenium** for selenium/webdriver support
* **nginx** for nginx container support

In the dependency description below, replace `--artifact name--` as appropriate and `--latest version--` with the [latest version available on Maven Central](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.testcontainers%22):

    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>--artifact name--</artifactId>
        <version>--latest version--</version>
    </dependency>

### JitPack (unreleased versions)

Alternatively, if you like to live on the bleeding edge, jitpack.io can be used to obtain SNAPSHOT versions.
Use the following dependency description instead:

	<dependency>
	    <groupId>com.github.testcontainers.testcontainers-java</groupId>
	    <artifactId>--artifact name--</artifactId>
	    <version>-SNAPSHOT</version>
	</dependency>

A specific git revision (such as `093a3a4628`) can be used as a fixed version instead. The JitPack maven repository must also be declared, e.g.:

	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
	
The [testcontainers examples project](https://github.com/testcontainers/testcontainers-java-examples) uses JitPack to fetch the latest, master version.

### Shaded dependencies

**Note**: Testcontainers uses the docker-java client library, which in turn depends on JAX-RS, Jersey and Jackson
libraries. These libraries in particular seem to be especially prone to conflicts with test code/applciation under test
 code. As such, **these libraries are 'shaded' into the core testcontainers JAR** and relocated
 under `org.testcontainers.shaded` to prevent class conflicts.

## Logging

Testcontainers, and many of the libraries it uses, utilize slf4j for logging. In order to see logs from Testcontainers,
your project should include an SLF4J implementation (Logback is recommended). The following example `logback-test.xml`
should be included in your classpath to show a reasonable level of log output:

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="info">
        <appender-ref ref="STDOUT"/>
    </root>

    <logger name="org.apache.http" level="WARN"/>
    <logger name="com.github.dockerjava" level="WARN"/>
    <logger name="org.zeroturnaround.exec" level="WARN"/>
</configuration>
```

## License

See [LICENSE](https://raw.githubusercontent.com/testcontainers/testcontainers-java/master/LICENSE).

## Attributions

This project includes a modified class (ScriptUtils) taken from the Spring JDBC project, adapted under the terms of the Apache license. Copyright for that class remains with the original authors.

This project was initially inspired by a [gist](https://gist.github.com/mosheeshel/c427b43c36b256731a0b) by Mosche Eschel.

## Contributing

* Star the project on [Github](https://github.com/testcontainers/testcontainers-java) and help spread the word :)
* See [ROADMAP](ROADMAP.md) to understand the approach behind the project and what may/may not be in store for the future.
* [Post an issue](https://github.com/testcontainers/testcontainers-java/issues) if you find any bugs
* Contribute improvements or fixes using a [Pull Request](https://github.com/testcontainers/testcontainers-java/pulls). If you're going to contribute, thank you! Please just be sure to:
	* discuss with the authors on an issue ticket prior to doing anything big
	* follow the style, naming and structure conventions of the rest of the project
	* make commits atomic and easy to merge
	* verify all tests are passing. Build the project with `mvn clean install -Pproprietary-deps` to do this.

## Copyright

Copyright (c) 2015, 2016 Richard North and other authors.

See [AUTHORS](https://raw.githubusercontent.com/testcontainers/testcontainers-java/master/AUTHORS) for contributors.
