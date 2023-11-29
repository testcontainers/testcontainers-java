# General Container runtime requirements

## Overview

To run Testcontainers-based tests, 
you need a Docker-API compatible container runtime, 
such as using [Testcontainers Cloud](https://www.testcontainers.cloud/) or installing Docker locally.
During development, Testcontainers is actively tested against recent versions of Docker on Linux,
as well as against Docker Desktop on Mac and Windows.
These Docker environments are automatically detected and used by Testcontainers without any additional configuration being necessary.

It is possible to configure Testcontainers to work with alternative container runtimes.
Making use of the free [Testcontainers Desktop](https://testcontainers.com/desktop/) app will take care of most of the manual configuration.
When using those alternatives without Testcontainers Desktop, 
sometimes some manual configuration might be necessary 
(see further down for specific runtimes, or [Customizing Docker host detection](/features/configuration/#customizing-docker-host-detection) for general configuration mechanisms).
Alternative container runtimes are not actively tested in the main development workflow,
so not all Testcontainers features might be available.
If you have further questions about configuration details for your setup or whether it supports running Testcontainers-based tests,
please contact the Testcontainers team and other users from the Testcontainers community on [Slack](https://slack.testcontainers.org/).

## Colima

In order to run testcontainers against [colima](https://github.com/abiosoft/colima) the env vars below should be set

```bash
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
export DOCKER_HOST="unix://${HOME}/.colima/default/docker.sock"
```

## Podman

In order to run testcontainers against [podman](https://podman.io/) the env vars bellow should be set

MacOS:

```bash
{% raw %}
export DOCKER_HOST=unix://$(podman machine inspect --format '{{.ConnectionInfo.PodmanSocket.Path}}')
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
{% endraw %}
```

Linux:

```bash
export DOCKER_HOST=unix://${XDG_RUNTIME_DIR}/podman/podman.sock
```

If you're running Podman in rootless mode, ensure to include the following line to disable Ryuk:

```bash
export TESTCONTAINERS_RYUK_DISABLED=true
```

!!! note
    Previous to version 1.19.0, `export TESTCONTAINERS_RYUK_PRIVILEGED=true`
    was required for rootful mode. Starting with 1.19.0, this is no longer required.

## Rancher Desktop

In order to run testcontainers against [Rancher Desktop](https://rancherdesktop.io/) the env vars below should be set.

If you're running Rancher Desktop as an administrator in a MacOS (M1) machine:

Using QEMU emulation

```bash
export TESTCONTAINERS_HOST_OVERRIDE=$(rdctl shell ip a show rd0 | awk '/inet / {sub("/.*",""); print $2}')
```

Using VZ emulation

```bash
export TESTCONTAINERS_HOST_OVERRIDE=$(rdctl shell ip a show vznat | awk '/inet / {sub("/.*",""); print $2}')
```

If you're not running Rancher Desktop as an administrator in a MacOS (M1) machine:

Using VZ emulation

```bash
export DOCKER_HOST=unix://$HOME/.rd/docker.sock
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
export TESTCONTAINERS_HOST_OVERRIDE=$(rdctl shell ip a show vznat | awk '/inet / {sub("/.*",""); print $2}')
```

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

## Docker registry authentication

Testcontainers will try to authenticate to registries with supplied config using the following strategies in order:

* Environment variables:
    * `DOCKER_AUTH_CONFIG`
* Docker config
	* At location specified in `DOCKER_CONFIG` or at `{HOME}/.docker/config.json`
