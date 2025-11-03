# Jupiter / JUnit 5

While Testcontainers is tightly coupled with the JUnit 4.x rule API, this module provides
an API that is based on the [JUnit Jupiter](https://junit.org/junit5/) extension model.

The extension supports two modes:

- containers that are restarted for every test method
- containers that are shared between all methods of a test class

Note that Jupiter/JUnit 5 integration is packaged as a separate library JAR; see [below](#adding-testcontainers-junit-5-support-to-your-project-dependencies) for details.

## Extension

Jupiter integration is provided by means of the `@Testcontainers` annotation.
  
The extension finds all fields that are annotated with `@Container` and calls their container lifecycle 
methods (methods on the `Startable` interface). Containers declared as static fields will be shared between test 
methods. They will be started only once before any test method is executed and stopped after the last test method has 
executed. Containers declared as instance fields will be started and stopped for every test method.
  
**Note:** This extension has only been tested with sequential test execution. Using it with parallel test execution is 
unsupported and may have unintended side effects.
  
*Example:*
<!--codeinclude-->
[Mixed Lifecycle](../../modules/junit-jupiter/src/test/java/org/testcontainers/junit/jupiter/MixedLifecycleTests.java) inside_block:testClass
<!--/codeinclude-->

## Examples

To use the Testcontainers extension annotate your test class with `@Testcontainers`.

### Restarted containers

To define a restarted container, define an instance field inside your test class and annotate it with
the `@Container` annotation.

<!--codeinclude-->
[Restarted Containers](../../modules/junit-jupiter/src/test/java/org/testcontainers/junit/jupiter/TestcontainersNestedRestartedContainerTests.java) inside_block:testClass
<!--/codeinclude-->


### Shared containers

Shared containers are defined as static fields in a top level test class and have to be annotated with `@Container`.
Note that shared containers can't be declared inside nested test classes.
This is because nested test classes have to be defined non-static and can't therefore have static fields.

<!--codeinclude-->
[Shared Container](../../modules/junit-jupiter/src/test/java/org/testcontainers/junit/jupiter/MixedLifecycleTests.java) lines:18-23,32-33,35-36
<!--/codeinclude-->

## Singleton containers

Note that the [singleton container pattern](manual_lifecycle_control.md#singleton-containers) is also an option when
using JUnit 5.

## Named Container Providers

The `@ContainerProvider` and `@ContainerConfig` annotations provide a declarative way to define and reuse containers across multiple tests and test classes, eliminating the need for manual singleton patterns.

### Overview

Container providers allow you to:
- Define containers once and reference them by name
- Share containers across multiple test classes
- Control container lifecycle (class-scoped or global)
- Choose between reusing instances or creating new ones per test
- Inject containers as test method parameters

### Basic Usage

Define a container provider method using `@ContainerProvider`:

```java
@Testcontainers
class MyIntegrationTests {

    @ContainerProvider(name = "redis", scope = Scope.GLOBAL)
    public GenericContainer<?> createRedis() {
        return new GenericContainer<>("redis:6.2")
            .withExposedPorts(6379);
    }

    @Test
    @ContainerConfig(name = "redis")
    void testWithRedis() {
        // Redis container is automatically started
    }
}
```

### Container Scopes

Containers can have two scopes:

- **`Scope.CLASS`**: Container is shared within a single test class and stopped after all tests in that class complete
- **`Scope.GLOBAL`**: Container is shared across all test classes and stopped at the end of the test suite

```java
@ContainerProvider(name = "database", scope = Scope.GLOBAL)
public PostgreSQLContainer<?> createDatabase() {
    return new PostgreSQLContainer<>("postgres:14");
}

@ContainerProvider(name = "cache", scope = Scope.CLASS)
public GenericContainer<?> createCache() {
    return new GenericContainer<>("redis:6.2");
}
```

### Parameter Injection

Containers can be injected as test method parameters:

```java
@Test
@ContainerConfig(name = "redis", injectAsParameter = true)
void testWithInjection(GenericContainer<?> redis) {
    String host = redis.getHost();
    int port = redis.getFirstMappedPort();
    // Use container...
}
```

### Creating New Instances

By default, containers are reused. Use `needNewInstance = true` for test isolation:

```java
@Test
@ContainerConfig(name = "database", needNewInstance = false)
void testSharedDatabase() {
    // Reuses existing database container
}

@Test
@ContainerConfig(name = "database", needNewInstance = true)
void testIsolatedDatabase() {
    // Gets a fresh database container
}
```

### Cross-Class Container Sharing

Containers can be shared across multiple test classes using inheritance:

```java
abstract class BaseIntegrationTest {
    @ContainerProvider(name = "sharedDb", scope = Scope.GLOBAL)
    public PostgreSQLContainer<?> createDatabase() {
        return new PostgreSQLContainer<>("postgres:14");
    }
}

@Testcontainers
class UserServiceTests extends BaseIntegrationTest {
    @Test
    @ContainerConfig(name = "sharedDb", injectAsParameter = true)
    void testUserService(PostgreSQLContainer<?> db) {
        // Uses shared database
    }
}

@Testcontainers
class OrderServiceTests extends BaseIntegrationTest {
    @Test
    @ContainerConfig(name = "sharedDb", injectAsParameter = true)
    void testOrderService(PostgreSQLContainer<?> db) {
        // Reuses the same database instance
    }
}
```

### Multiple Providers

A test class can define multiple container providers:

```java
@Testcontainers
class MultiContainerTests {

    @ContainerProvider(name = "postgres", scope = Scope.GLOBAL)
    public PostgreSQLContainer<?> createPostgres() {
        return new PostgreSQLContainer<>("postgres:14");
    }

    @ContainerProvider(name = "redis", scope = Scope.GLOBAL)
    public GenericContainer<?> createRedis() {
        return new GenericContainer<>("redis:6.2");
    }

    @Test
    @ContainerConfig(name = "postgres", injectAsParameter = true)
    void testDatabase(PostgreSQLContainer<?> db) {
        // Use postgres
    }

    @Test
    @ContainerConfig(name = "redis", injectAsParameter = true)
    void testCache(GenericContainer<?> cache) {
        // Use redis
    }
}
```

### Static vs Instance Provider Methods

Provider methods can be either static or instance methods:

```java
// Static provider - no test instance needed
@ContainerProvider(name = "static", scope = Scope.GLOBAL)
public static GenericContainer<?> createStatic() {
    return new GenericContainer<>("httpd:2.4");
}

// Instance provider - can access test instance fields
@ContainerProvider(name = "instance", scope = Scope.CLASS)
public GenericContainer<?> createInstance() {
    return new GenericContainer<>("httpd:2.4");
}
```

### Compatibility with @Container

Named providers work alongside traditional `@Container` fields:

```java
@Testcontainers
class MixedApproachTests {

    @Container
    private static final GenericContainer<?> TRADITIONAL = 
        new GenericContainer<>("httpd:2.4");

    @ContainerProvider(name = "provided", scope = Scope.CLASS)
    public GenericContainer<?> createProvided() {
        return new GenericContainer<>("redis:6.2");
    }

    @Test
    void testTraditional() {
        // Use TRADITIONAL container
    }

    @Test
    @ContainerConfig(name = "provided")
    void testProvided() {
        // Use provided container
    }
}
```

## Limitations

Since this module has a dependency onto JUnit Jupiter and on Testcontainers core, which
has a dependency onto JUnit 4.x, projects using this module will end up with both, JUnit Jupiter
and JUnit 4.x in the test classpath.

This extension has only been tested with sequential test execution. Using it with parallel test execution is unsupported and may have unintended side effects.

## Adding Testcontainers JUnit 5 support to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:testcontainers-junit-jupiter:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-junit-jupiter</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
