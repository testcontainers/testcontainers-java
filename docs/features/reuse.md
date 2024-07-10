# Reusable Containers (Experimental)

!!! warning 
    Reusable Containers is still an experimental feature and the behavior can change.
    Those containers won't stop after all tests are finished.

The *Reusable* feature keeps the containers running and next executions with the same container configuration
will reuse it. To use it, start the container manually by calling `start()` method, do not call `stop()` method
directly or indirectly via `try-with-resources` or `JUnit integration`, and enable it manually through an
opt-in mechanism per environment. To reuse a container, the container configuration **must be the same**.

!!! note
    Reusable containers are not suited for CI usage and as an experimental feature
    not all Testcontainers features are fully working (e.g., resource cleanup
    or networking).

## How to use it

* Enable `Reusable Containers` 
  * through environment variable `TESTCONTAINERS_REUSE_ENABLE=true` 
  * through user property file `~/.testcontainers.properties`, by adding `testcontainers.reuse.enable=true` 
  * **not** through classpath properties file [see this comment](https://github.com/testcontainers/testcontainers-java/issues/5364#issuecomment-1125907734)

* Define a container and subscribe to reuse the container using `withReuse(true)`

```java
GenericContainer container = new GenericContainer("redis:6-alpine")
    .withExposedPorts(6379)
    .withReuse(true)
```

* Start the container manually by using `container.start()`

### Reusable Container with Testcontainers JDBC URL

If using the [Testcontainers JDBC URL support](../../modules/databases/jdbc#database-containers-launched-via-jdbc-url-scheme)
the URL **must** follow the pattern of `jdbc:tc:mysql:8.0.36:///databasename?TC_REUSABLE=true`.
`TC_REUSABLE=true` is set as a parameter of the JDBC URL.
