# Windows Support (Alpha)

## Prerequisites
* [Docker for Windows](https://docs.docker.com/docker-for-windows/) needs to be installed
  * Docker version 17.06 is confirmed to work on Windows 10 with Hyper-V.
  * Testcontainers supports communication with Docker on Docker for Windows using named pipes.

## Limitations
The following features are not available or do not work correctly so make sure you do not use them or use them with 
caution. The list may not be complete.

### Testing

Testcontainers is not regularly tested on Windows, so please consider it to be at an beta level of readiness.

If you wish to use Testcontainers on Windows, please confirm that it works correctly for you before investing significant
effort.

### MySQL containers
* MySQL server prevents custom configuration file (ini-script) from being loaded due to security measures ([link to feature description](database_containers.md#using-an-init-script))

### Windows Container

* Windows and LCOW containers support is experimental, please [report](https://github.com/testcontainers/testcontainers-java/issues) any found problems.

### Docker credential helpers

Testcontainers supports credential helpers on Linux and MacOS, to allow pulling images from private registries. This feature is disabled on Windows due to lack of testing - if it is of value to you please help us test it. See [\#756](https://github.com/testcontainers/testcontainers-java/issues/756).

## Reporting issues

Please report any issues with the Windows build of Testcontainers [here](https://github.com/testcontainers/testcontainers-java/issues)
and be sure to note that you are using this on Windows.
