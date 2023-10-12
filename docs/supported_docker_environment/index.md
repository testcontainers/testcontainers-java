# General Docker requirements

## Overview

Testcontainers requires a Docker-API compatible container runtime. 
During development, Testcontainers is actively tested against recent versions of Docker on Linux, as well as against Docker Desktop on Mac and Windows. 
These Docker environments are automatically detected and used by Testcontainers without any additional configuration being necessary.

It is possible to configure Testcontainers to work for other Docker setups, such as a remote Docker host or Docker alternatives. 
However, these are not actively tested in the main development workflow, so not all Testcontainers features might be available and additional manual configuration might be necessary. 
If you have further questions about configuration details for your setup or whether it supports running Testcontainers-based tests, 
please contact the Testcontainers team and other users from the Testcontainers community on [Slack](https://slack.testcontainers.org/).

| Host Operating System / Environment | Minimum recommended docker versions | Known issues / tips                                                                                                                                                                                                                                                                       |
|-------------------------------------|-----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Linux - general                     | Docker v17.09              | After docker installation, follow [post-installation steps](https://docs.docker.com/engine/install/linux-postinstall/).                                                                                                                                                                   |
| Linux - CircleCI (LXC driver)      | Docker v17.09               | The `exec` feature is not compatible with CircleCI. See CircleCI configuration [example](./continuous_integration/circle_ci.md)                                                                                                                                                           |
| Linux - within a Docker container            | Docker v17.09              | See [Running inside Docker](continuous_integration/dind_patterns.md) for Docker-in-Docker and Docker wormhole patterns                                                                                                                                                                    |
| Mac OS X - Docker Toolbox           | Docker Machine v0.8.0  |                                                                                                                                                                                                                                                                                           |
| Mac OS X - Docker for Mac      | v17.09          | Starting 4.13, run `sudo ln -s $HOME/.docker/run/docker.sock /var/run/docker.sock`<br>Support is best-efforts at present<br>`getTestHostIpAddress()` is [not currently supported](https://github.com/testcontainers/testcontainers-java/issues/166) due to limitations in Docker for Mac. |
| Windows - Docker Toolbox            |                             | *Support is limited at present and this is not currently tested on a regular basis*.                                                                                                                                                                                                      |
| Windows - Docker for Windows   |                             | *Support is best-efforts at present.* Only Linux Containers (LCOW) are supported at the moment. See [Windows Support](windows.md)                                                                                                                                                         |
| Windows - Windows Subsystem for Linux (WSL) | Docker v17.09                       | *Support is best-efforts at present.* Only Linux Containers (LCOW) are supported at the moment. See [Windows Support](windows.md).                                                                                                                                                        |

## Using Colima?

In order to run testcontainers against [colima](https://github.com/abiosoft/colima) the env vars bellow should be set

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
