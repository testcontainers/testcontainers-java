# Reusable Containers (Experimental)

!!! warning 
    Reusable containers is still an experimental feature and the behavior can change.
    Those containers won't stop after all tests are finished.

Reusable containers enables to keep containers running. In order
to achieve it, manual initialization should be used by calling `start()`
and should be enabled by environment. In order to reuse a container, the
configuration *must not change*.

!!! note
    Reusable containers doesn't suit for CI usage and as an experimental feature
    not all Testcontainers features are working such as cleanup, parallelization
    or networking.

## How to use it

* Define container with `withReuse(true)`

```java
GenericContainer container = new GenericContainer("redis:6-alpine")
    .withExposedPorts(6379)
    .withReuse(true)
```

* If using Testcontainers JDBC URL support *should* pass `TC_REUSABLE=true`  parameter
* Opt-in to reuse in `~/.testcontainers.properties`, adding `testcontainers.reuse.enable=true`
* Containers should be started manually using `container.start()`. See [docs](https://www.testcontainers.org/test_framework_integration/manual_lifecycle_control/)
