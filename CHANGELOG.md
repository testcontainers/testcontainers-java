# Change Log
All notable changes to this project will be documented in this file.

## [1.0.0]
### Fixed
- Resolve Jersey/Jackson dependency clashes by shading (relocating) a version of these libraries into the core Testcontainers JAR
- Improve documentation and logging concerning discovery of Docker daemon

### Changed
- Rename container `getIpAddress()` method to `getContainerIpAddress()` and deprecate original method name.
- Rename container `getHostIpAddress()` method to `getTestHostIpAddress()`

## [0.9.9]
### Fixed
- Resolve thread safety issues associated with use of a singleton docker client
- Resolve disk space check problems when running on a Debian-based docker host
- Fix CircleCI problems where the build could hit memory limits

### Changed
- Remove bundled logback.xml to allow users more control over logging
- Add Travis CI support for improved breadth of testing

## [0.9.8]
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

## [0.9.7]
### Added
- Support for overriding MySQL container configuration (my.cnf file overrides)

### Changed
- Replace dependency on org.testpackage with org.rnorth.visible-assertions

## [0.9.6]
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
