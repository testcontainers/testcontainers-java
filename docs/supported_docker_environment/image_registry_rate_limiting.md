# Image Registry rate limiting

As of November 2020 Docker Hub pulls are rate limited. 
As Testcontainers uses Docker Hub for standard images, some users may hit these rate limits and should mitigate accordingly.

Suggested mitigations are noted in [this issue](https://github.com/testcontainers/testcontainers-java/issues/3099) at present.

## Which images are used by Testcontainers?

As of the current version of Testcontainers ({{latest_version}}):

* every image directly used by your tests
* images pulled by Testcontainers itself to support functionality:
    * [`testcontainers/ryuk`](https://hub.docker.com/r/testcontainers/ryuk) - performs fail-safe cleanup of containers, and always required (unless [Ryuk is disabled](../features/configuration.md#disabling-ryuk))
    * [`alpine`](https://hub.docker.com/r/_/alpine) - used to check whether images can be pulled at startup, and always required (unless [startup checks are disabled](../features/configuration.md#disabling-the-startup-checks))
    * [`testcontainers/sshd`](https://hub.docker.com/r/testcontainers/sshd) - required if [exposing host ports to containers](../features/networking.md#exposing-host-ports-to-the-container)
    * [`testcontainers/vnc-recorder`](https://hub.docker.com/r/testcontainers/vnc-recorder) - required if using [Webdriver containers](../modules/webdriver_containers.md) and using the screen recording feature
    * [`docker/compose`](https://hub.docker.com/r/docker/compose) - required if using [Docker Compose](../modules/docker_compose.md)
    * [`alpine/socat`](https://hub.docker.com/r/alpine/socat) - required if using [Docker Compose](../modules/docker_compose.md)
