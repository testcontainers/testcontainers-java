# Docker Compose

## Benefits

Similar to [generic containers](generic_containers.md) support, it's also possible to run a bespoke set of services
specified in a `docker-compose.yml` file.

This is intended to be useful on projects where Docker Compose is already used in dev or other environments to define
services that an application may be dependent upon.

Behind the scenes, Testcontainers actually launches a temporary Docker Compose client - in a container, of course, so
it's not necessary to have it installed on all developer/test machines.

## Example

A single class rule, pointing to a `docker-compose.yml` file, should be sufficient to launch any number of services
required by your tests:
```java
@ClassRule
public static DockerComposeContainer environment =
    new DockerComposeContainer(new File("src/test/resources/compose-test.yml"))
            .withExposedService("redis_1", REDIS_PORT)
            .withExposedService("elasticsearch_1", ELASTICSEARCH_PORT);
```

In this example, `compose-test.yml` should have content such as:
```yaml
redis:
  image: redis
elasticsearch:
  image: elasticsearch
```

Note that it is not necessary to define ports to be exposed in the YAML file; this would inhibit reuse/inclusion of the
file in other contexts.

Instead, Testcontainers will spin up a small 'ambassador' container, which will proxy
between the Compose-managed containers and ports that are accessible to your tests. This is done using a separate, minimal
container that runs socat as a TCP proxy.

## Accessing a container from tests

The rule provides methods for discovering how your tests can interact with the containers:

* `getServiceHost(serviceName, servicePort)` returns the IP address where the container is listening (via an ambassador
    container)
* `getServicePort(serviceName, servicePort)` returns the Docker mapped port for a port that has been exposed (via an
    ambassador container)

For example, with the Redis example above, the following will allow your tests to access the Redis service:
```java
String redisUrl = environment.getServiceHost("redis_1", REDIS_PORT)
                    + ":" +
                  environment.getServicePort("redis_1", REDIS_PORT);
```

## Startup timeout
Ordinarily Testcontainers will wait for up to 60 seconds for each exposed container's first mapped network port to start listening.

This simple measure provides a basic check whether a container is ready for use.

There are overloaded `withExposedService` methods that take a `WaitStrategy` so you can specify a timeout strategy per container.

### Waiting for startup examples

Waiting for exposed port to start listening:
```java
@ClassRule
public static DockerComposeContainer environment =
    new DockerComposeContainer(new File("src/test/resources/compose-test.yml"))
            .withExposedService("redis_1", REDIS_PORT, 
                Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)));
```

Wait for arbitrary status codes on an HTTPS endpoint:
```java
@ClassRule
public static DockerComposeContainer environment =
    new DockerComposeContainer(new File("src/test/resources/compose-test.yml"))
            .withExposedService("elasticsearch_1", ELASTICSEARCH_PORT, 
                Wait.forHttp("/all")
                    .forStatusCode(200)
                    .forStatusCode(401)
                    .usingTls());
```

Separate wait strategies for each container:
```java
@ClassRule
public static DockerComposeContainer environment =
    new DockerComposeContainer(new File("src/test/resources/compose-test.yml"))
            .withExposedService("redis_1", REDIS_PORT, Wait.forListeningPort())
            .withExposedService("elasticsearch_1", ELASTICSEARCH_PORT, 
                Wait.forHttp("/all")
                    .forStatusCode(200)
                    .forStatusCode(401)
                    .usingTls());
```

Alternatively, you can use `waitingFor(serviceName, waitStrategy)`, 
for example if you need to wait on a log message from a service, but don't need to expose a port.

```java
@ClassRule
public static DockerComposeContainer environment =
    new DockerComposeContainer(new File("src/test/resources/compose-test.yml"))
            .withExposedService("redis_1", REDIS_PORT, Wait.forListeningPort())
            .waitingFor("db_1", Wait.forLogMessage("started", 1));
```

## Using private repositories in Docker compose
When Docker Compose is used in container mode (not local), it's needs to be made aware of Docker settings for private repositories. 
By default, those setting are located in `$HOME/.docker/config.json`. 

There are 3 ways to specify location of the `config.json` for Docker Compose
* Use `DOCKER_CONFIG_FILE` environment variable. 

    `export DOCKER_CONFIG_FILE=/some/location/config.json` 

* Use `dockerConfigFile` java property
    
    `java -DdockerConfigFile=/some/location/config.json`

* Don't specify anything, in this case default location `$HOME/.docker/config.json`, if present, will be used 

####Note to OSX users
By default, Docker for mac uses Keychain to store private repositories' keys. So, your `config.json` looks like
```$json
{
  "auths" : {
    "https://index.docker.io/v1/" : {
    }
  },
  "credsStore" : "osxkeychain"
}
```

Docker Compose in container cannot access the Keychain, thus making the configuration useless. 
To work around this problem, create `config.json` in separate location with real authentication keys, like 
```$json
{
  "auths" : {
    "https://index.docker.io/v1/" : {
     "auth": "QWEADSZXC..."
    }
  },
  "credsStore" : "osxkeychain"
}
```
and specify the location to Testcontainers using any of the two first methods from above. 
