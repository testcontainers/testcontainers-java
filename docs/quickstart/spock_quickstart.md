# Spock Quickstart

It's easy to add Testcontainers to your project - let's walk through a quick example to see how.

Let's imagine we have a simple program that has a dependency on Redis, and we want to add some tests for it.
In our imaginary program, there is a `RedisBackedCache` class which stores data in Redis.
 
You can see an example test that could have been written for it (without using Testcontainers):

<!--codeinclude-->
[Pre-Testcontainers test code](../examples/spock/redis/src/test/groovy/quickstart/RedisBackedCacheIntTestStep0.groovy) block:RedisBackedCacheIntTestStep0
<!--/codeinclude-->

Notice that the existing test has a problem - it's relying on a local installation of Redis, which is a red flag for test reliability.
This may work if we were sure that every developer and CI machine had Redis installed, but would fail otherwise.
We might also have problems if we attempted to run tests in parallel, such as state bleeding between tests, or port clashes.

Let's start from here, and see how to improve the test with Testcontainers:  

## 1. Add Testcontainers as a test-scoped dependency

First, add Testcontainers as a dependency as follows:

```groovy tab='Gradle'
testImplementation "org.testcontainers:spock:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>spock</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```

## 2. Get Testcontainers to run a Redis container during our tests

Annotate the Spock specification class with the Testcontainers extension:

```groovy tab='Spock Testcontainers annotation'
@org.testcontainers.spock.Testcontainers
class RedisBackedCacheIntTest extends Specification {
```

And add the following field to the body of our test class:

<!--codeinclude-->
[Spock Testcontainers init](../examples/spock/redis/src/test/groovy/quickstart/RedisBackedCacheIntTest.groovy) inside_block:init
<!--/codeinclude-->

This tells Spock to start a Testcontainers `GenericContainer`, configured to use a specific Redis image from Docker Hub, and configured to expose a port.

If we run our test as-is, then regardless of the actual test outcome, we'll see logs showing us that Testcontainers:

* was activated before our test method ran
* discovered and quickly tested our local Docker setup
* pulled the image if necessary
* started a new container and waited for it to be ready
* shut down and deleted the container after the test

## 3. Make sure our code can talk to the container

Before Testcontainers, we might have hardcoded an address like `localhost:6379` into our tests.

Testcontainers uses *randomized ports* for each container it starts, but makes it easy to obtain the actual port at runtime.
We can do this in our test `setup` method, to set up our component under test:

<!--codeinclude-->
[Obtaining a mapped port](../examples/spock/redis/src/test/groovy/quickstart/RedisBackedCacheIntTest.groovy) inside_block:setup
<!--/codeinclude-->

!!! tip
    Notice that we also ask Testcontainers for the container's actual address with `redis.containerIpAddress`, 
    rather than hard-coding `localhost`. `localhost` may work in some environments but not others - for example it may
    not work on your current or future CI environment. As such, **avoid hard-coding** the address, and use 
    `containerIpAddress` instead.

## 4. Run the tests!

That's it!

Let's look at our complete test class to see how little we had to add to get up and running with Testcontainers:

<!--codeinclude-->
[RedisBackedCacheIntTest](../examples/spock/redis/src/test/groovy/quickstart/RedisBackedCacheIntTest.groovy) block:complete
<!--/codeinclude-->
