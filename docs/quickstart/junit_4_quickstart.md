# JUnit 4 Quickstart

It's easy to add Testcontainers to your project - let's walk through a quick example to see how.

Let's imagine we have a simple program that has a dependency on Redis, and we want to add some tests for it.
In our imaginary program, there is a `RedisBackedCache` class which stores data in Redis.
 
You can see an example test that could have been written for it (without using Testcontainers):

<!--codeinclude-->
[Pre-Testcontainers test code](../examples/junit4/redis/src/test/java/quickstart/RedisBackedCacheIntTestStep0.java) block:RedisBackedCacheIntTestStep0
<!--/codeinclude-->

Notice that the existing test has a problem - it's relying on a local installation of Redis, which is a red flag for test reliability.
This may work if we were sure that every developer and CI machine had Redis installed, but would fail otherwise.
We might also have problems if we attempted to run tests in parallel, such as state bleeding between tests, or port clashes.

Let's start from here, and see how to improve the test with Testcontainers:  

## 1. Add Testcontainers as a test-scoped dependency

First, add Testcontainers as a dependency as follows:

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

## 2. Get Testcontainers to run a Redis container during our tests

Simply add the following to the body of our test class:

<!--codeinclude-->
[JUnit 4 Rule](../examples/junit4/redis/src/test/java/quickstart/RedisBackedCacheIntTest.java) inside_block:rule
<!--/codeinclude-->

The `@Rule` annotation tells JUnit to notify this field about various events in the test lifecycle.
In this case, our rule object is a Testcontainers `GenericContainer`, configured to use a specific Redis image from Docker Hub, and configured to expose a port.

If we run our test as-is, then regardless of the actual test outcome, we'll see logs showing us that Testcontainers:

* was activated before our test method ran
* discovered and quickly tested our local Docker setup
* pulled the image if necessary
* started a new container and waited for it to be ready
* shut down and deleted the container after the test

## 3. Make sure our code can talk to the container

Before Testcontainers, we might have hardcoded an address like `localhost:6379` into our tests.

Testcontainers uses *randomized ports* for each container it starts, but makes it easy to obtain the actual port at runtime.
We can do this in our test `setUp` method, to set up our component under test:

<!--codeinclude-->
[Obtaining a mapped port](../examples/junit4/redis/src/test/java/quickstart/RedisBackedCacheIntTest.java) inside_block:setUp
<!--/codeinclude-->

!!! tip
    Notice that we also ask Testcontainers for the container's actual address with `redis.getHost();`, 
    rather than hard-coding `localhost`. `localhost` may work in some environments but not others - for example it may
    not work on your current or future CI environment. As such, **avoid hard-coding** the address, and use 
    `getHost()` instead.

## 4. Run the tests!

That's it!

Let's look at our complete test class to see how little we had to add to get up and running with Testcontainers:

<!--codeinclude-->
[RedisBackedCacheIntTest](../examples/junit4/redis/src/test/java/quickstart/RedisBackedCacheIntTest.java) block:RedisBackedCacheIntTest
<!--/codeinclude-->

