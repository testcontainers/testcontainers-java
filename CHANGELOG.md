# Change Log
All notable changes to this project will be documented in this file.

## [1.2.1] - 2017-04-06
### Fixed
- Fix bug in space detection when `alpine:3.5` image has not yet been pulled (#323, #324)
- Minor documentation fixes

### Changed
- Add AOP Alliance dependencies to shaded deps to reduce chance of conflicts (#315)

## [1.2.0] - 2017-03-12
### Fixed
- Fix various escaping issues that may arise when paths contain spaces (#263, #279)
- General documentation fixes/improvements (#300, #303, #304)
- Improve reliability of `ResourceReaper` when there are a large number of containers returned by `docker ps -a` (#295)

### Changed
- Support Docker for Windows via TCP socket connection (#291, #297, #309). _Note that Docker Compose is not yet supported under Docker for Windows (see #306)
- Expose `docker-java`'s `CreateContainerCmd` API for low-level container tweaking (#301)
- Shade `org.newsclub` and Guava dependencies (#299, #292)
- Add `org.testcontainers` label to all containers created by Testcontainers (#294)

## [1.1.9] - 2017-02-12
### Fixed
- Fix inability to run Testcontainers on Alpine linux. Unix-socket-over-TCP is now used in linux environments where netty fails due to lack of glibc libraries (#290)
- Fix slow feedback in the case of missing JDBC drivers by failing-fast if the required driver cannot be found (#280, #230)

### Changed
- Add ability to change 'tiny image' used for disk space checks (#287)
- Add ability to attach volumes to a container using 'volumes from' (#244, #289)

## [1.1.8] - 2017-01-22
### Fixed 
- Compatibility fixes for Docker for Mac v1.13.0 (#272)
- Relax docker environment disk space check to accomodate unusual empty `df` output observed on Docker for Mac with OverlayFS (#273, #278)
- Fix inadvertent private-scoping of startup checks' `StartupStatus`, which made implementation of custom startup checks impossible (#266)
- Fix potential resource lead/deadlock when errors are encountered building images from a Dockerfile (#274)

### Changed
- Add support for execution within a Docker container (#267), correcting resolution of container addresses
- Add support for version 2 of private docker registries, configured via `$HOME/.docker/config.json` (#270)
- Use current classloader instead of system classloader for loading JDBC drivers (#261)
- Allow hardcoded container image names for Ambassador and VNC recorder containers to be changed via a configuration file (#277, #259)
- Allow Selenium Webdriver container image name to be specified as a constructor parameter (#249, #171)


## [1.1.7] - 2016-11-19
### Fixed
- Compensate for premature TCP socket opening in Docker for Mac (#160, #236)
- (Internal) Stabilise various parts of Testcontainers' self test suite (#241)
- Fix mounting of classpath resources when those resources are in a JAR file (#213)
- Reduce misleading error messages caused mainly by trying to perform operations on stopped containers (#243) 

### Changed
- Uses a default MySQL and MariaDB configuration to reduce memory footprint (#209, #243)
- Docker Compose can optionally now use a local `docker-compose` executable rather than running inside a container (#200)
- Add support for privileged mode containers (#234, #235)
- Allow container/network cleanup (ResourceReaper) to be triggered programmatically (#231)
- Add optional tailing of logs for containers spawned by Docker Compose (#233) 
- (Internal) Relocate non-proprietary database container tests to a single module

## [1.1.6] - 2016-09-22
### Fixed
- Fix logging of discovered Docker environment variables (#218)
- Adopt longer timeout periods for testing docker client configurations, and allow these to be further customised through system properties (#217, see *ClientProviderStrategy classes)
- Fix docker compose directory mounting on windows (#224)
- Handle and ignore further categories of failure in retrieval of docker environment disk space (#225)

### Changed
- Add extra configurability options (database name, username, password) for PostgreSQL DB containers (#220)
- Add MariaDB container type (#215)
- Use Docker Compose `down` action for more robust teardown of compose environments
- Ensure that Docker Compose operations run sequentially rather than concurrently if JUnit tests are parallelized (#226)
- Allow multiple Docker Compose files to be specified, to allow for extension/composition of services (#227)

## [1.1.5] - 2016-08-22
### Fixed
- Fix Docker Compose environment variable passthrough (#208)

### Changed
- Remove Docker Compose networks when containers are shut down (#211) as well as at JVM shutdown

## [1.1.4] - 2016-08-16
### Fixed
- Fix JDBC proxy driver behaviour when used with Tomcat connection pool to avoid spawning excessive numbers of containers (#195)
- Shade Jersey dependencies in JDBC module to avoid classpath conflicts (#202)
- Fix NullPointerException when docker host has untagged images (#201)
- Fix relative paths for volumes mounted in docker-compose containers (#189)

### Changed
- Update to v3.0.2 of docker-java library
- Switch to a shared, single instance docker client rather than a separate client instance per container rule (#193)
- Ensure that docker-compose pulls images (with no timeout), prior to trying to start (#188)
- Use official `docker/compose` image for running docker-compose (#190)

## [1.1.3] - 2016-07-27
### Fixed
- Further fix for shading of netty Linux native libs, specifically when run using Docker Compose support
- Ensure that file mode permissions are retained for Dockerfile builder

### Changed
- Add support for specifying container working directory, and set this to match the `/compose` directory for Docker Compose
- Improve resilience of Selenium container startup
- Add `withLogConsumer(...)` to allow a log consumer to be attached to a container from the moment of startup

## [1.1.2] - 2016-07-19
### Fixed
- Fix shading of netty Linux native libs

### Changed
- Shade guava artifacts to prevent classloader conflicts

## [1.1.1] - 2016-07-17
### Fixed
- Improve shutdown of unnecessary docker clients (#170)
- Shade `io.netty` dependencies into the testcontainers core JAR to reduce conflicts (#170 and #157)
- Remove timeouts for docker compose execution, particularly useful when image pulls are involved
- Improve output logging from docker-compose, pausing to log output in case of failure rather than letting logs intermingle.

### Changed
- Reinstate container startup retry (removed in v1.1.0) as an optional setting, only used by default for Selenium webdriver containers

## [1.1.0] - 2016-07-05
### Fixed
- Apply shade relocation to Jersey repackaged Guava libs
- General logging and stability improvements to Docker Compose support
- Fix liveness checks to use specific IP address obtained using `getContainerIpAddress()`

### Changed
- Integrate interim support for Docker for Mac beta and Docker Machine for Windows. See [docs](docs/index.md) for known limitations.
- Add support for Docker Compose v2 and scaling of compose containers
- Add support for attaching containers to specific networks.
- Allow container environment variables to be set using a Map

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

[1.2.0]: https://github.com/testcontainers/testcontainers-java/releases/tag/testcontainers-1.2.0
[1.1.9]: https://github.com/testcontainers/testcontainers-java/releases/tag/testcontainers-1.1.9
[1.1.8]: https://github.com/testcontainers/testcontainers-java/releases/tag/testcontainers-1.1.8
[1.1.7]: https://github.com/testcontainers/testcontainers-java/releases/tag/testcontainers-1.1.7
[1.1.6]: https://github.com/testcontainers/testcontainers-java/releases/tag/testcontainers-1.1.6
[1.1.5]: https://github.com/testcontainers/testcontainers-java/releases/tag/testcontainers-1.1.5
[1.1.4]: https://github.com/testcontainers/testcontainers-java/releases/tag/testcontainers-1.1.4
[1.1.3]: https://github.com/testcontainers/testcontainers-java/releases/tag/testcontainers-1.1.3
[1.1.2]: https://github.com/testcontainers/testcontainers-java/releases/tag/testcontainers-1.1.2
[1.1.1]: https://github.com/testcontainers/testcontainers-java/releases/tag/testcontainers-1.1.1
[1.1.0]: https://github.com/testcontainers/testcontainers-java/releases/tag/testcontainers-1.1.0
[1.0.5]: https://github.com/testcontainers/testcontainers-java/releases/tag/testcontainers-1.0.5
[1.0.4]: https://github.com/testcontainers/testcontainers-java/releases/tag/testcontainers-1.0.4
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
