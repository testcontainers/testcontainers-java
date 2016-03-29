# Generic containers

## Benefits

Testcontainers' generic container support offers the most flexibility, and makes it easy to use virtually any container
images as temporary test dependencies. For example, if you might use it to test interactions with:

* NoSQL databases or other data stores (e.g. redis, elasticsearch, mongo)
* Web servers/proxies (e.g. nginx, apache)
* Log services (e.g. logstash, kibana)
* Other services developed by your team/organization which are already dockerized

## Example

A generic container rule can be used with any public docker image; for example:

    // Set up a redis container
    @ClassRule
    public static GenericContainer redis =
    	new GenericContainer("redis:3.0.2")
                   .withExposedPorts(6379);


    // Set up a plain OS container and customize environment, 
    //   command and exposed ports. This just listens on port 80 
    //   and always returns '42'
    @ClassRule
    public static GenericContainer alpine =
    	new GenericContainer("alpine:3.2")
        		.withExposedPorts(80)
                   .withEnv("MAGIC_NUMBER", "42")
                   .withCommand("/bin/sh", "-c", 
                   "while true; do echo \"$MAGIC_NUMBER\" | nc -l -p 80; done");

These containers, as `@ClassRule`s, will be started before any tests in the class run, and will be destroyed after all
tests have run.

## Accessing a container from tests

The class rule provides methods for discovering how your tests can interact with the containers:

* `getIpAddress()` returns the IP address where the container is listening
* `getMappedPort(...)` returns the Docker mapped port for a port that has been exposed on the container

For example, with the Redis example above, the following will allow your tests to access the Redis service:

    String redisUrl = redis.getIpAddress() + ":" + redis.getMappedPort(6379);

## Options

### Image

With a generic container, you set the container image using a parameter to the rule constructor, e.g.:

	new GenericContainer("jboss/wildfly:9.0.1.Final")

### Exposing ports

If you need to expose ports on the container, use the `withExposedPorts` method on the rule passing integer port numbers:

	new GenericContainer(...)
			.withExposedPorts(22, 80, 8080)

or strings (optionally specifying tcp):

	new GenericContainer(...)
			.withExposedPorts("22", "80/tcp", "8080/tcp")

### Environment variables

To add environment variables to the container, use `withEnv`:

	new GenericContainer(...)
			.withEnv("API_TOKEN", "foo")

### Command

By default the container will execute whatever command is specified in the image's Dockerfile. To override this, and specify a different command, use `withCommand`:

	new GenericContainer(...)
	        .withCommand("/app/start.sh")

### Volume mapping

It is possible to map a file or directory **on the classpath** into the container as a volume using `withClasspathResourceMapping`:

	new GenericContainer(...)
            .withClasspathResourceMapping("redis.conf",
                                          "/etc/redis.conf",
                                          BindMode.READ_ONLY)

### Executing a command

Your test can execute a command inside a running container, similar to a `docker exec` call:

   myContainer.execInContainer("touch", "/tmp/foo");

This can be useful for software that has a command line administration tool. You can also get the output from the command:

   ExecResult result = myContainer.execInContainer("tail", "-1", "/var/logs/foo");
   assertThat(result.getStdout().contains("message"));

There are two limitations:
* There's no way to get the return code of the executed command
* This isn't supported if your docker daemon uses the older "lxc" execution engine.
