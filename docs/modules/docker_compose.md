# Docker Compose Module

## Benefits

Similar to generic containers support, it's also possible to run a bespoke set of services
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

## 'Local compose' mode

You can override Testcontainers' default behaviour and make it use a `docker-compose` binary installed on the local machine. 
This will generally yield an experience that is closer to running docker-compose locally, with the caveat that Docker Compose needs to be present on dev and CI machines.
```java
public static DockerComposeContainer environment =
    new DockerComposeContainer(new File("src/test/resources/compose-test.yml"))
            .withExposedService("redis_1", REDIS_PORT, Wait.forListeningPort())
            .waitingFor("db_1", Wait.forLogMessage("started", 1))
            .withLocalCompose(true);
```
## Using private repositories in Docker compose
When Docker Compose is used in container mode (not local), it's needs to be made aware of Docker settings for private repositories. 
By default, those setting are located in `$HOME/.docker/config.json`. 

There are 3 ways to specify location of the `config.json` for Docker Compose:

* Use `DOCKER_CONFIG_FILE` environment variable. 

    `export DOCKER_CONFIG_FILE=/some/location/config.json`

* Use `dockerConfigFile` java property
    
    `java -DdockerConfigFile=/some/location/config.json`

* Don't specify anything. In this case default location `$HOME/.docker/config.json`, if present, will be used.

!!! note "Docker Compose and Credential Store / Credential Helpers"
    Modern Docker tends to store credentials using the credential store/helper mechanism rather than storing credentials in Docker's configuration file. So, your `config.json` may look something like:
    
    ```json
    {
      "auths" : {
        "https://index.docker.io/v1/" : {
        }
      },
      "credsStore" : "osxkeychain"
    }
    ```
    
    When run inside a container, Docker Compose cannot access the Keychain, thus making the configuration useless. 
    To work around this problem, there are two options:
    
    ##### Putting auths in a config file
    Create a `config.json` in separate location with real authentication keys, like:
    
    ```json
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
    
    ##### Using 'local compose' mode
    
    [Local Compose mode](#local-compose-mode), mentioned above, will allow compose to directly access the Docker auth system (to the same extent that running the `docker-compose` CLI manually works).
    

## Adding this module to your project dependencies

*Docker Compose support is part of the core Testcontainers library.*

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testImplementation "org.testcontainers:testcontainers:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```

