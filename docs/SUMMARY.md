# Summary

* [Introduction](index.md)
* [Compatibility](compatibility.md)
* [Usage](usage.md)
    * [Prerequisites](usage.md#prerequisites)
    * [Docker environment discovery](usage.md#docker-environment-discovery)
    * [Usage modes](usage.md#usage-modes)
    * [Maven dependencies](usage.md#maven-dependencies)
    * [Logging](usage.md#logging)
    * [Properties](usage/properties.md)
    * [Running inside Docker](usage/inside_docker.md)

## Generic containers

* [Benefits](usage/generic_containers.md#benefits)
* [Example](usage/generic_containers.md#example)
* [Accessing a container from tests](usage/generic_containers.md#accessing-a-container-from-tests)
* [Options](usage/options.md)
    * [Specifying image name](usage/options.md#image)
    * [Exposing ports](usage/options.md#exposing-ports)
    * [Environment variables](usage/options.md#environment-variables)
    * [Command](usage/options.md#command)
    * [Volume mapping](usage/options.md#volume-mapping)
    * [Customizing the container](usage/options.md#customizing-the-container)
    * [Startup timeout](usage/options.md#startup-timeout)
    * [Following container output](usage/options.md#following-container-output)
    * [Executing a command](usage/options.md#executing-a-command)


## Specialised container types

* [Temporary database containers](usage/database_containers.md)
    * [Benefits](usage/database_containers.md#benefits)
    * [Examples and options](usage/database_containers.md#examples-and-options)
    * [JUnit rule](usage/database_containers.md#junit-rule)
    * [JDBC URL](usage/database_containers.md#jdbc-url)
    * [Using an init script](usage/database_containers.md#using-an-init-script)

* [Selenium WebDriver containers](usage/webdriver_containers.md)
    * [Benefits](usage/webdriver_containers.md#benefits)
    * [Example](usage/webdriver_containers.md#example)
    * [Other browsers](usage/webdriver_containers.md#other-browsers)
    * [Recording videos](usage/webdriver_containers.md#recording-videos)

* [Docker Compose](usage/docker_compose.md)
* [Dockerfile containers](usage/dockerfile.md)
* [Windows support](usage/windows_support.md)

## Continuous Integration

* [GitLab CI](ci/ci.md#gitlab)
* [Circle CI](ci/ci.md#circleci-20)

## Examples
* [Selenium](https://github.com/testcontainers/testcontainers-java-examples/blob/master/selenium-container/src/test/java/SeleniumContainerTest.java)
* [Custom Redis container](https://github.com/testcontainers/testcontainers-java-examples/blob/master/redis-backed-cache/src/test/java/RedisBackedCacheTest.java)
* [Spring Boot testing with Redis](https://github.com/testcontainers/testcontainers-java-examples/tree/master/spring-boot/src/test/java/com/example)
## 
* [License](index.md#license)
* [Attributions](index.md#attributions)
* [Contributing](index.md#contributing)
