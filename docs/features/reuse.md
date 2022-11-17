# Reusable Containers (Experimental)

!!! warning 
    Reusable Containers is still an experimental feature and the behavior can change.
    Those containers won't stop after all tests are finished.

The *Reusable Containers* feature keeps the containers running between test sessions. In order
to use it, manual container lifecycle instrumentation should be used by calling the `start()` method
and it needs to be manually enabled through an opt-in mechanism. In order to reuse a container, the
configuration of the container *must not change*.

!!! note
    Reusable containers are not suited for CI usage and as an experimental feature
    not all Testcontainers features are fully working (e.g., resource cleanup
    or networking).

## How to use it

* Define a container with `withReuse(true)`

```java
GenericContainer container = new GenericContainer("redis:6-alpine")
    .withExposedPorts(6379)
    .withReuse(true)
```

* Opt-in to Reusable Containers in `~/.testcontainers.properties`, by adding `testcontainers.reuse.enable=true`
* Containers need to be started manually using `container.start()`. See [docs](../../test_framework_integration/manual_lifecycle_control)

### Reusable Container with Testcontainers JDBC URL

If using the [Testcontainers JDBC URL support](../../modules/databases/jdbc#database-containers-launched-via-jdbc-url-scheme)
the URL **must** follow the pattern of `jdbc:tc:mysql:5.7.34:///databasename?TC_REUSABLE=true`. `TC_REUSABLE=true` is set as a parameter of the JDBC URL.
