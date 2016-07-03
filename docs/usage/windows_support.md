# Windows Support (Alpha)

## Prerequisites
* [Docker Toolbox for Windows](https://docs.docker.com/engine/installation/windows/) needs to be installed
* Currently Docker for Windows without Toolbox (Linux VM) is not supported

## Limitations
The following features are not available or do not work correctly so make sure you do not use them or use them with 
caution. The list may not be complete.

### Testing

Testcontainers is not regularly tested on Windows, so please consider it to be at an alpha level of readiness.

If you wish to use Testcontainers on Windows, please confirm that it works correctly for you before investing significant
effort.

### MySQL containers
* MySQL server prevents custom configuration file (ini-script) from being loaded due to security measures ([link to feature description](database_containers.md#using-an-init-script))

## Reporting issues

Please report any issues with the Windows build of Testcontainers [here](https://github.com/testcontainers/testcontainers-java/issues)
and be sure to note that you are using this on Windows.