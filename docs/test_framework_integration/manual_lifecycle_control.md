# Manual container lifecycle control

Testcontainers is fully usable with any test framework, or with no framework at all.

## Manually starting/stopping containers

Containers can be started and stopped in code using `start()` and `stop()` methods. Additionally, container classes
implement `AutoCloseable`. This enables better assurance that the container will be stopped at the appropriate time.

```java
try (GenericContainer container = new GenericContainer("imagename")) {
    container.start();
    // ... use the container
    // no need to call stop() afterwards
}
```

## Singleton containers

Sometimes it might be useful to define a container that is only started once for several test classes.
There is no special support for this use case provided by the Testcontainers extension.
Instead this can be implemented using the following pattern:

<!--codeinclude-->
[Singleton container base class](../../examples/singleton-container/src/test/java/com/example/AbstractIntegrationTest.java) block:AbstractIntegrationTest
<!--/codeinclude-->

For a complete set of test classes using this pattern, see the [singleton container example](https://github.com/testcontainers/testcontainers-java/tree/main/examples/singleton-container).

The singleton container is started only once when the base class is loaded.
The container can then be used by all inheriting test classes.
At the end of the test suite the [Ryuk container](https://github.com/testcontainers/moby-ryuk)
that is started by Testcontainers core will take care of stopping the singleton container.
