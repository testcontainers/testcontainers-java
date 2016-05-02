# Change Log
All notable changes to this project will be documented in this file.

## [1.0.5] - 2016-05-02
### Fixed
- Fix problems associated with changes to `tenforce/virtuoso:latest` container, and replace with a pinned version.
- Fix build-time dependency on visible-assertions library, which had downstream dependencies that started to break the Testcontainers build.

### Changed
- Add support for pluggable wait strategies, i.e. overriding the default TCP connect wait strategy with HTTP ping or any user-defined approach.
- Add 'self-typing' to allow easy use of fluent-style options even when `GenericContainer` is subclassed.
- Add support for defining extra entries for containers' `/etc/hosts` files.
- Add fluent setter for setting file-system file/directory binding

## [1.0.4] - 2016-04-17
### Fixed
- Prevent unnecessary and erroneous reconfiguration of container if startup needs to be retried
- Consolidate container cleanup to ensure that ambassador containers used for Docker Compose are cleaned up appropriately
- Fix container liveness check port lookup for FixedHostPortGenericContainer.
- Upgrade docker-compose container to dduportal/docker-compose:1.6.0 for compatibility with docker compose file format v2.

### Changed
- Add `docker exec` support for running commands against running containers
- Add support for building container images on the fly from Dockerfiles, including optional Dockerfile builder DSL
- Add container name as prefix for container logs that are streamed to SLF4J
- Improve container startup failure detection, including adding the option to specify a minimum up time that the container should achieve before being considered started successfully

## [1.0.3] - 2016-03-31
### Fixed
- Resolve issues where containers would not be cleaned up on JVM shutdown if they failed to start correctly
- Fix validation problem where docker image names that contained private registry URLs with port number would be rejected
- Resolve bug where `docker pull` would try infinitely for a non-existent image name

### Changed
- Set startup free disk space check to ensure that the Docker environment has a minimum of 2GB available rather than 10%
- Add streaming of container logs to SLF4J loggers, capture as Strings, and also the ability to wait for container log content to satisfy an expected predicate
- Allow configuration of docker container startup timeout
- Add detection of classpath Selenium version, and automatic selection of correct Selenium docker containers for compatibility

## [1.0.2] - 2016-02-27
### Fixed
- If a container fail to start up correctly, startup will now be retried up to a limit of 3 times
- Add resilience around `getMappedPort` method to fail fast when a port is not yet mapped, rather than generate misleading errors

### Changed
- Add JDBC container module for OpenLink Virtuoso
- Add additional debug level logging to aid with diagnosis of docker daemon discovery problems
- Add support for using a local Unix socket to connect to the Docker daemon

## [1.0.1] - 2016-02-18
### Fixed
- Remove extraneous service loader entries in the shaded JAR
- Upgrade to v2.2.0 of docker-java client library to take advantage of unix socket fixes (see https://github.com/docker-java/docker-java/issues/456)
- Validate that docker image names include a tag on creation

### Changed
- By default, use docker machine name from `DOCKER_MACHINE_NAME` environment, or `default` if it exists
- Allow container ports to map to a fixed port on the host through use of the `FixedHostPortGenericContainer` subclass of `GenericContainer`

## [1.0.0] - 2016-02-07
### Fixed
- Resolve Jersey/Jackson dependency clashes by shading (relocating) a version of these libraries into the core Testcontainers JAR
- Improve documentation and logging concerning discovery of Docker daemon

### Changed
- Rename container `getIpAddress()` method to `getContainerIpAddress()` and deprecate original method name.
- Rename container `getHostIpAddress()` method to `getTestHostIpAddress()`

## [0.9.9] - 2016-01-12
### Fixed
- Resolve thread safety issues associated with use of a singleton docker client
- Resolve disk space check problems when running on a Debian-based docker host
- Fix CircleCI problems where the build could hit memory limits

### Changed
- Remove bundled logback.xml to allow users more control over logging
- Add Travis CI support for improved breadth of testing

## [0.9.8] - 2015-08-12
### Changed
- Change from Spotify docker client library to docker-java, for improved compatibility with latest versions of Docker
- Change from JDK 1.7 minimum requirement to JDK 1.8
- Replace boot2docker support with docker-machine support
- Docker images are now prefetched when a @Rule is instantiated
- Combined Rule and Container classes throughout, for a reduced set of public classes and removal of some duplication
- Improvements to container cleanup, especially removal of data volumes
- General improvements to error handling, logging etc throughout

### Added
- Docker Compose support
- Automatic docker environment disk space check

## [0.9.7] - 2015-08-07
### Added
- Support for overriding MySQL container configuration (my.cnf file overrides)

### Changed
- Replace dependency on org.testpackage with org.rnorth.visible-assertions

## [0.9.6] - 2015-07-22
### Added
- Generic container support (allows use of any docker image) using a GenericContainerRule.

### Changed
- Renamed from org.rnorth.test-containers to org.testcontainers
- Explicit support for usage on linux and use with older versions of Docker (v1.2.0 tested)

## [0.9.5] - 2015-06-28
### Added
- Oracle XE container support

### Changed
- Support for JDK 1.7 (previously was JDK 1.8+)

## [0.9.4] and 0.9.3 - 2015-06-23
### Changed
- Refactored for better modularization

## [0.9.2] - 2015-06-13
### Added
- 'Sidekick' VNC recording container to record video of Selenium test sessions

### Changed
- Alter timezone used for time display inside Selenium containers

## [0.9.1] - 2015-06-07
### Added
- Support for Selenium webdriver containers
- Recording of Selenium test sessions using vnc2flv

## [0.9] - 2015-04-29
Initial release

[1.0.3]: https://github.com/testcontainers/testcontainers-java/releases/tag/testcontainers-1.0.3
[1.0.2]: https://github.com/testcontainers/testcontainers-java/releases/tag/testcontainers-1.0.2
[1.0.1]: https://github.com/testcontainers/testcontainers-java/releases/tag/testcontainers-1.0.1
[1.0.0]: https://github.com/testcontainers/testcontainers-java/releases/tag/testcontainers-1.0.0
[0.9.9]: https://github.com/testcontainers/testcontainers-java/releases/tag/testcontainers-0.9.9
[0.9.8]: https://github.com/testcontainers/testcontainers-java/releases/tag/testcontainers-0.9.8
[0.9.7]: https://github.com/testcontainers/testcontainers-java/releases/tag/test-containers-0.9.7
[0.9.6]: https://github.com/testcontainers/testcontainers-java/releases/tag/test-containers-0.9.6
[0.9.5]: https://github.com/testcontainers/testcontainers-java/releases/tag/test-containers-0.9.5
[0.9.4]: https://github.com/testcontainers/testcontainers-java/releases/tag/test-containers-0.9.4
[0.9.3]: https://github.com/testcontainers/testcontainers-java/releases/tag/test-containers-0.9.3
[0.9.2]: https://github.com/testcontainers/testcontainers-java/releases/tag/test-containers-0.9.2
[0.9.1]: https://github.com/testcontainers/testcontainers-java/releases/tag/test-containers-0.9.1
[0.9]: https://github.com/testcontainers/testcontainers-java/releases/tag/test-containers-0.9
