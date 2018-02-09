# Custom properties

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
> **tinyimage.container.image = alpine:3.5**  
> Used by Testcontainers' core

> **vncrecorder.container.image = richnorth/vnc-recorder:latest**  
> Used by VNC recorder in Testcontainers' Seleniun integration

> **ambassador.container.image = richnorth/ambassador:latest**  
> **compose.container.image = docker/compose:1.8.0**  
> Used by Docker Compose integration

> **kafka.container.image = confluentinc/cp-kafka**  
> Used by KafkaContainer 

Testcontainers uses public Docker images to perform different actions like startup checks, VNC recording and others.  
Some companies disallow the usage of Docker Hub, but you can override `*.image` properties with your own images from your private registry to workaround that.
