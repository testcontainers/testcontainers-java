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

!!! warning "Spring Boot and the test context cache"
    Under `@SpringBootTest`, this pattern is usually required rather than merely an optimisation.

    Spring caches the application context and reuses it across test classes, while the JUnit 5 `@Testcontainers` extension ties container lifecycle to the test *class*. When both apply: the first test class starts a container and Spring builds a context holding its mapped port; the extension stops that container when the class finishes; the next test class then reuses the **cached** context, which still points at the container that was just stopped. Every test in it fails with connection errors.

    The symptom is misleading, because the first class passes and later ones fail with errors that mention connections rather than containers — so it tends to read like test pollution or a test ordering problem.

    Starting the container in a static initialiser and never stopping it keeps it alive for as long as the cached context can be reused. Ryuk still removes it when the JVM exits, so nothing is leaked.
