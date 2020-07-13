# Windows Support

## Prerequisites
* [Docker for Windows](https://docs.docker.com/docker-for-windows/) needs to be installed
  * Docker version 17.06 is confirmed to work on Windows 10 with Hyper-V.
  * Testcontainers supports communication with Docker on Docker for Windows using named pipes.
  * WSL2 backend is supported starting with Windows 10 2004. (**Beta**)
  * Docker on Windows Server 2019 is currently **not supported** (also note [this issue](https://github.com/testcontainers/testcontainers-java/issues/2960)).

## Limitations
The following features are not available or do not work correctly so make sure you do not use them or use them with 
caution. The list may not be complete.

### MySQL containers
* MySQL server prevents custom configuration file (ini-script) from being loaded due to security measures ([link to feature description](../modules/databases/index.md#using-an-init-script-from-a-file))

### Windows Container on Windows (WCOW)

* WCOW is currently not supported, since Testcontainers uses auxiliary Linux containers for certain tasks and Docker for Windows does not support hybrid engine mode at the time of writing.

## WSL2 backend

Using Docker for Windows with WSL2 backend should work out of the box.

However, there is an [existing issue](https://github.com/microsoft/WSL/issues/4694) in WSL/WSL2 that effects certain older Docker images. 
The currently proposed workaround is to enable vsyscall emulation in the WSL2 kernel:
```
[wsl2]
kernelCommandLine = vsyscall=emulate
```

## Windows Subsystem for Linux (WSL)

Testcontainers supports communicating with Docker for Windows within the Windows Subsystem for Linux *([**WSL**](https://docs.microsoft.com/en-us/windows/wsl/about))*.
The following additional configurations steps are required:

+ Expose the Docker for Windows daemon on tcp port `2375` without **TLS**.
  *(Right-click the Docker for Windows icon on the task bar, click setting and go to `General`)*.

+ Set the `DOCKER_HOST` environment variable inside the **WSL** shell to `tcp://localhost:2375`.
  It is recommended to add this to your `~/.bashrc` file, so it’s available every time you open your terminal.

+ **Optional** - Only if volumes are required:  
  Inside the **WSL** shell, modify the `/ect/wsl.conf` file to mount the Windows drives on `/` instead of on `/mnt/`.
  *(Reboot required after this step)*.  
  Remember to share the drives, on which you will store your volumes, with Docker for Windows.
  *(Right-click the Docker for Windows icon on the task bar, click setting and go to `Shared Drives`)*.

More information about running Docker within the **WSL** can be found [here](https://nickjanetakis.com/blog/setting-up-docker-for-windows-and-wsl-to-work-flawlessly).

## Reporting issues

Please report any issues with the Windows build of Testcontainers [here](https://github.com/testcontainers/testcontainers-java/issues)
and be sure to note that you are using this on Windows.
