# TestContainers

> TestContainers is a Java library that supports JUnit tests, providing lightweight, throwaway instances of common databases, Selenium web browsers, or anything else that can run in a Docker container.

## Table of Contents
<!-- MarkdownTOC autolink=true bracket=round depth=3 -->

- [Use Cases](#use-cases)
- [Usage summary](#usage-summary)
- [Usage](#usage)
    - [Prerequisites](#prerequisites)
    - [Usage modes](usage-modes)
- [Maven dependencies](#maven-dependencies)
- [Supported containers](#supported-containers)
- [License](#license)
- [Attributions](#attributions)
- [Contributing](#contributing)
- [Copyright](#copyright)

<!-- /MarkdownTOC -->

## Use Cases

TestContainers makes it easy to launch useful Docker containers for the duration of JUnit tests.

 * **Data access layer integration tests**: use a containerized instance of a MySQL, PostgreSQL or Oracle database to test your data access layer code for complete compatibility, but without requiring complex setup on developers' machines and safe in the knowledge that your tests will always start with a known DB state. Any other database type that can be containerized can also be used.
 * **Application integration tests**: for running your application in a short-lived test mode with dependencies, such as databases, message queues or web servers.
 * **UI/Acceptance tests**: use containerized web browsers, compatible with Selenium, for conducting automated UI tests. Each test can get a fresh instance of the browser, with no browser state, plugin variations or automated browser upgrades to worry about. And you get a video recording of each test session, or just each session where tests failed.

## Usage

### Prerequisites

Docker or boot2docker (for OS X) must be installed on the machine you are running tests on. TestContainers currently requires JDK 1.7 and is compatible with JUnit and Selenium2/WebDriver.

### Usage modes

* [Temporary database containers](usage/database_containers) - specialized MySQL, PostgreSQL and Oracle XE container support
* [Webdriver containers](usage/webdriver_containers) - run a Dockerized Chrome or Firefox browser ready for Selenium/Webdriver operations - complete with automatic video recording
* [Generic containers](usage/generic_containers) - run any Docker container as a test dependency
* [Docker compose](usage/docker_compose) - reuse services defined in a Docker Compose YAML file

## Maven dependencies

TestContainers is distributed in a handful of modules:

* **junit** for just core functionality and generic containers support
* **mysql**, **postgresql** or **oracle-xe** for database container support
* **selenium** for selenium support

In the dependency description below, replace `--artifact name--` as appropriate:


    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>--artifact name--</artifactId>
        <version>0.9.6</version>
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
* See [ROADMAP](ROADMAP) to understand the approach behind the project and what may/may not be in store for the future.
* [Post an issue](https://github.com/testcontainers/testcontainers-java/issues) if you find any bugs
* Contribute improvements or fixes using a [Pull Request](https://github.com/testcontainers/testcontainers-java/pulls). If you're going to contribute, thank you! Please just be sure to:
	* discuss with the authors on an issue ticket prior to doing anything big
	* follow the style, naming and structure conventions of the rest of the project
	* make commits atomic and easy to merge
	* verify all tests are passing. Build the project with `mvn clean install -Pproprietary-deps` to do this.

## Copyright

Copyright (c) 2015 Richard North and other authors.

See [AUTHORS](AUTHORS) for contributors.