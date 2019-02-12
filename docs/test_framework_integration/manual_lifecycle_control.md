# Manual container lifecycle control

While Testcontainers was originally built with JUnit 4 integration in mind, it is fully usable with other test 
frameworks, or with no framework at all.

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

```java
abstract class AbstractContainerBaseTest {

    static final MySQLContainer MY_SQL_CONTAINER;

    static {
        MY_SQL_CONTAINER = new MySQLContainer();
        MY_SQL_CONTAINER.start();
    }
}

class FirstTest extends AbstractContainerBaseTest {

    @Test
    void someTestMethod() {
        String url = MY_SQL_CONTAINER.getJdbcUrl();

        // create a connection and run test as normal
    }
}
```

The singleton container is started only once when the base class is loaded.
The container can then be used by all inheriting test classes.
At the end of the test suite the [Ryuk container](https://github.com/testcontainers/moby-ryuk)
that is started by Testcontainers core will take care of stopping the singleton container.
