# Custom configuration

You can override some default properties if your environment requires that.

## Configuration file location
The configuration will be loaded from multiple locations. Properties are considered in the following order:

1. `.testcontainers.properties` in user's home folder. Example locations:  
**Linux:** `/home/myuser/.testcontainers.properties`  
**Windows:** `C:/Users/myuser/.testcontainers.properties`  
**macOS:** `/Users/myuser/.testcontainers.properties`
2. `testcontainers.properties` on classpath

## Disabling the startup checks
> **checks.disable = [true|false]**

Before running any containers Testcontainers will perform a set of startup checks to ensure that your environment is configured correctly. Usually they look like this:
```
        ℹ︎ Checking the system...
        ✔ Docker version should be at least 1.6.0
        ✔ Docker environment should have more than 2GB free disk space
        ✔ File should be mountable
        ✔ A port exposed by a docker container should be accessible
```
It takes a couple of seconds, but if you want to speed up your tests, you can disable the checks once you have everything configured. Add `checks.disable=true` to your `$HOME/.testcontainers.properties` to completely disable them.

## Customizing images

Testcontainers uses public Docker images to perform different actions like startup checks, VNC recording and others. 
Some companies disallow the usage of Docker Hub, but you can override `*.image` properties with your own images from your private registry to workaround that.

> **tinyimage.container.image = alpine:3.5**  
> Used by Testcontainers' core

> **vncrecorder.container.image = quay.io/testcontainers/vnc-recorder:1.1.0**  
> Used by VNC recorder in Testcontainers' Seleniun integration

> **ambassador.container.image = richnorth/ambassador:latest**  
> **compose.container.image = docker/compose:1.8.0**  
> Used by Docker Compose integration

> **kafka.container.image = confluentinc/cp-kafka**  
> Used by KafkaContainer 

## Customizing Ryuk resource reaper

> **ryuk.container.image = quay.io/testcontainers/ryuk:0.2.3**
> The resource reaper is responsible for container removal and automatic cleanup of dead containers at JVM shutdown

> **ryuk.container.privileged = false**
> In some environments ryuk must be started in privileged mode to work properly (--privileged flag)

### Disabling Ryuk
Ryuk must be started as a privileged container.  
If your environment already implements automatic cleanup of containers after the execution,
but does not allow starting privileged containers, you can turn off the Ryuk container by setting
`TESTCONTAINERS_RYUK_DISABLED` **environment variable** to `true`.

!!!tip
    Note that Testcontainers will continue doing the cleanup at JVM's shutdown, unless you `kill -9` your JVM process.

