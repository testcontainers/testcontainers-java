# General Docker requirements

## Overview

| Host Operating System / Environment | Minimum recommended docker versions | Known issues / tips                                                                                                                                                                                                                                                                                                                                                |
|-------------------------------------|-----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Linux - general                     | Docker v1.10              |                                                                                                                                                                                                                                                                                                                                                                    |
| Linux - Travis CI                   | Docker v1.10              | See [example .travis.yml](https://raw.githubusercontent.com/testcontainers/testcontainers-java/master/.travis.yml) for baseline Travis CI configuration                                                                                                                                                                                                                                                                                     |
| Linux - Circle CI (LXC driver)      | Docker v1.9.1               | The `exec` feature is not compatible with Circle CI. See [example circle.yml](https://raw.githubusercontent.com/testcontainers/testcontainers-java/master/circle.yml) for baseline CircleCI configuration                                                                                                                                                                                                                                   |
| Linux - within a Docker container            | Docker v1.12              | See [Running inside Docker](continuous_integration/dind_patterns.md) for Docker-in-Docker and Docker wormhole patterns                                                                                                                                                                                                                              |
| Mac OS X - Docker Toolbox           | Docker Machine v0.8.0  |                                                                                                                                                                                                                                                                                                                                                                    |
| Mac OS X - Docker for Mac      | 1.12.0          | *Support is best-efforts at present*. `getTestHostIpAddress()` is [not currently supported](https://github.com/testcontainers/testcontainers-java/issues/166) due to limitations in Docker for Mac. |
| Windows - Docker Toolbox            |                             | *Support is limited at present and this is not currently tested on a regular basis*.                                                                                                                                                                                                                                                                               |
| Windows - Docker for Windows   |                             | *Support is best-efforts at present.* Only Linux Containers (LCOW) are supported at the moment. See [Windows Support](windows.md)                                                                                                                                                                                                                                                                                                                                        |
| Windows - Windows Subsystem for Linux (WSL) | Docker v17.06                       | *Support is best-efforts at present.* Only Linux Containers (LCOW) are supported at the moment. See [Windows Support](windows.md). |

## Docker environment discovery

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
* If you're going to run your tests inside a container, please read [Patterns for running tests inside a docker container](continuous_integration/dind_patterns.md) first.
