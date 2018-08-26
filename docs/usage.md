# Usage

### Prerequisites

Docker or docker-machine (for OS X) must be installed on the machine you are running tests on. Testcontainers currently requires JDK 1.8 and is compatible with JUnit.

If you want to use Testcontainers on Windows you can try the [alpha release](usage/windows_support.md).

### Docker environment discovery

Testcontainers will try to connect to a Docker daemon using the following strategies in order:

* Environment variables:
	* `DOCKER_HOST`
	* `DOCKER_TLS_VERIFY`
	* `DOCKER_CERT_PATH`
* Defaults:
	* `DOCKER_HOST=https://localhost:2376`
	* `DOCKER_TLS_VERIFY=1`
	* `DOCKER_CERT_PATH=~/.docker`
* If Docker Machine is installed, the docker machine environment for the *first* machine found. Docker Machine needs to be on the PATH for this to succeed.
* If you're going to run your tests inside a container, please read [Running inside a Docker container](usage/inside_docker.md) first.

### Usage modes

* [Temporary database containers](usage/database_containers.md) - specialized Microsoft SQL Server, MariaDB, MySQL, PostgreSQL, Oracle XE and Virtuoso container support
* [Webdriver containers](usage/webdriver_containers.md) - run a dockerized Chrome or Firefox browser ready for Selenium/Webdriver operations - complete with automatic video recording
* [Kafka containers](usage/kafka_containers.md) - run a dockerized Kafka, a distributed streaming platform
* [Generic containers](usage/generic_containers.md) - run any Docker container as a test dependency
* [Docker compose](usage/docker_compose.md) - reuse services defined in a Docker Compose YAML file
* [Dockerfile containers](usage/dockerfile.md) - run a container that is built on-the-fly from a Dockerfile

## Maven dependencies

Testcontainers is distributed in a handful of Maven modules:

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

    <logger name="org.testcontainers" level="INFO"/>
    <logger name="org.apache.http" level="WARN"/>
    <logger name="com.github.dockerjava" level="WARN"/>
    <logger name="org.zeroturnaround.exec" level="WARN"/>
</configuration>
```

## Using custom utility images

Testcontainers uses additional docker images under some modes of execution: 

* `richnorth/ambassador:latest`, which is a TCP proxy used to expose Docker Compose container ports outside of the compose network.
* `richnorth/vncrecorder:latest`, which is a VNC->FLV recorder, used for capturing Webdriver test videos.

> *N.B.:* both these images use the 'latest' tag, which could potentially affect repeatability of tests and compatibility with Testcontainers _if the image is ever changed_. This is a [known issue](https://github.com/testcontainers/testcontainers-java/issues/276) which will be addressed in the future. The current 'latest' version of these images will never be changed until they are replaced by a new image altogether.

Last but not least, `alpine:3.2` image is used for Docker host IP address detection in some special cases.

If it is necessary to override these image names (e.g. when using a private registry), you should create a file named `testcontainers.properties` and place it on the classpath with the following content:

```properties
ambassador.container.image=replacement image name here
vncrecorder.container.image=replacement image name here
tinyimage.container.image=replacement image name here
```
