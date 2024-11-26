# Docker Compose Module

## Benefits

Similar to generic container support, it's also possible to run a bespoke set of services specified in a 
`docker-compose.yml` file. 

This is especially useful for projects where Docker Compose is already used in development 
or other environments to define services that an application may be dependent upon.

The `ComposeContainer` leverages [Compose V2](https://www.docker.com/blog/announcing-compose-v2-general-availability/),
making it easy to use the same dependencies from the development environment within tests.

## Example

A single class `ComposeContainer`, defined based on a `docker-compose.yml` file, 
should be sufficient to launch any number of services required by our tests:

<!--codeinclude-->
[Create a ComposeContainer](../../core/src/test/java/org/testcontainers/junit/ComposeContainerTest.java) inside_block:composeContainerConstructor
<!--/codeinclude-->

!!! note
    Make sure the service names use a `-` rather than `_` as separator.

In this example, Docker Compose file should have content such as:
```yaml
services:
  redis:
    image: redis
  db:
    image: mysql:8.0.36
```

Note that it is not necessary to define ports to be exposed in the YAML file, 
as this would inhibit the reuse/inclusion of the file in other contexts.

Instead, Testcontainers will spin up a small `ambassador` container, 
which will proxy between the Compose-managed containers and ports that are accessible to our tests. 

## ComposeContainer vs DockerComposeContainer 

So far, we discussed `ComposeContainer`, which supports docker compose [version 2](https://www.docker.com/blog/announcing-compose-v2-general-availability/). 

On the other hand, `DockerComposeContainer` utilizes Compose V1, which has been marked deprecated by Docker.

The two APIs are quite similar, and most examples provided on this page can be applied to both of them.

## Accessing a Container

ComposeContainer provides methods for discovering how your tests can interact with the containers:

* `getServiceHost(serviceName, servicePort)` returns the IP address where the container is listening (via an ambassador
    container)
* `getServicePort(serviceName, servicePort)` returns the Docker mapped port for a port that has been exposed (via an
    ambassador container)

Let's use this API to create the URL that will enable our tests to access the Redis service:
<!--codeinclude-->
[Access a Service's host and port](../../core/src/test/java/org/testcontainers/junit/ComposeContainerTest.java) inside_block:getServiceHostAndPort
<!--/codeinclude-->

## Wait Strategies and Startup Timeouts
Ordinarily Testcontainers will wait for up to 60 seconds for each exposed container's first mapped network port to start listening.
This simple measure provides a basic check whether a container is ready for use.

There are overloaded `withExposedService` methods that take a `WaitStrategy` 
where we can specify a timeout strategy per container. 

We can either use the fluent API to crate a [custom strategy](../features/startup_and_waits.md) or use one of the already existing ones, 
accessible via the static factory methods from of the `Wait` class.

For instance, we can wait for exposed port and set a custom timeout:
<!--codeinclude-->
[Wait for the exposed port and use a custom timeout](../../core/src/test/java/org/testcontainers/junit/ComposeContainerWithWaitStrategies.java) inside_block:composeContainerWaitForPortWithTimeout
<!--/codeinclude-->

Needless to say, we can define different strategies for each service in our Docker Compose setup. 

For example, our Redis container can wait for a successful redis-cli command, 
while our db service waits for a specific log message:

<!--codeinclude-->
[Wait for a custom command and a log message](../../core/src/test/java/org/testcontainers/junit/ComposeContainerWithWaitStrategies.java) inside_block:composeContainerWithCombinedWaitStrategies
<!--/codeinclude-->



## The 'Local Compose' Mode

We can override Testcontainers' default behaviour and make it use a `docker-compose` binary installed on the local machine. 

This will generally yield an experience that is closer to running _docker compose_ locally, 
with the caveat that Docker Compose needs to be present on dev and CI machines.

<!--codeinclude-->
[Use ComposeContainer in 'Local Compose' mode](../../core/src/test/java/org/testcontainers/containers/ComposeProfilesOptionTest.java) inside_block:composeContainerWithLocalCompose
<!--/codeinclude-->

## Build Working Directory

We can select what files should be copied only via `withCopyFilesInContainer`:

<!--codeinclude-->
[Use ComposeContainer in 'Local Compose' mode](../../core/src/test/java/org/testcontainers/junit/ComposeContainerWithCopyFilesTest.java) inside_block:composeContainerWithCopyFiles
<!--/codeinclude-->

In this example, only docker compose and env files are copied over into the container that will run the Docker Compose file.  
By default, all files in the same directory as the compose file are copied over.

We can use file and directory references. 
They are always resolved relative to the directory where the compose file resides.

!!! note
    This can be used with `DockerComposeContainer` and `ComposeContainer`, but **only in the containerized Compose (not with `Local Compose` mode)**.

## Using private repositories in Docker compose
When Docker Compose is used in container mode (not local), it needs to be made aware of Docker
settings for private repositories. 
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

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:testcontainers:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

