# Windows Support

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
* MySQL server prevents custom configuration file (ini-script) from being loaded due to security measures ([link to feature description](../modules/databases/index.md#using-an-init-script))

### Windows Container on Windows (WCOW)

* WCOW is currently not supported, since Testcontainers uses auxiliary Linux containers for certain tasks and Docker for Windows does not support hybrid engine mode at the time of writing.

## Windows Subsystem for Linux

Testcontainers supports comunicatin with Docker for Windows within the Windows Subsystem for Linux *([**WSL**](https://docs.microsoft.com/en-us/windows/wsl/about))*.
The following additional configurations steps are required:

+ The Docker for windows daemon needs to be exposed on the tcp port `2375` without **TLS**.
+ Set the `DOCKER_HOST` enviroment variable as `tcp://localhost:2375`.
+ Modify the `/ect/wsl.conf` file to mount the windows drivers on `/` instead of on `/mnt/`.

More information about running Docker within the **WSL** can be found [here](https://nickjanetakis.com/blog/setting-up-docker-for-windows-and-wsl-to-work-flawlessly).

## Reporting issues

Please report any issues with the Windows build of Testcontainers [here](https://github.com/testcontainers/testcontainers-java/issues)
and be sure to note that you are using this on Windows.
