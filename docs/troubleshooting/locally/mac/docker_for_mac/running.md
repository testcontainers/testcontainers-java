# Do you see the following lines in the log output?

```
11:49:17.755 INFO  org.testcontainers.dockerclient.DockerClientProviderStrategy - Found Docker environment with Environment variables, system properties and defaults. Resolved: 
    dockerHost=unix:///var/run/docker.sock
    apiVersion='{UNKNOWN_VERSION}'
    registryUrl='https://index.docker.io/v1/'
    registryUsername='****'
    registryPassword='null'
    registryEmail='null'
    dockerConfig='DefaultDockerClientConfig[dockerHost=unix:///var/run/docker.sock,registryUsername=****,registryPassword=<null>,registryEmail=<null>,registryUrl=https://index.docker.io/v1/,dockerConfigPath=/Users/****/.docker,sslConfig=<null>,apiVersion={UNKNOWN_VERSION},dockerConfig=<null>]'

11:49:17.765 INFO  org.testcontainers.DockerClientFactory - Docker host IP address is localhost
11:49:17.916 INFO  org.testcontainers.DockerClientFactory - Connected to docker: 
  Server Version: 18.09.1
  API Version: 1.39
  Operating System: Docker for Mac
  Total Memory: 16024 MB
        ℹ︎ Checking the system...
        ✔ Docker version should be at least 1.6.0
```

||
|-|
|[Yes](../../../past_checks.md)|
|[No, I don't see any logs](../../../results/logging.md)|
|[No](../../../todo.md)|
