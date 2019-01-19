# Jupiter / JUnit 5


While Testcontainers is tightly coupled with the JUnit 4.x rule API, this module provides
an API that is based on the [JUnit Jupiter](https://junit.org/junit5/) extension model.

The extension supports two modes:

- containers that are restarted for every test method
- containers that are shared between all methods of a test class


## Extension

Jupiter integration is provided by means of the `@Testcontainers` annotation.
  
The extension finds all fields that are annotated with `@Container` and calls their container lifecycle 
methods (methods on the `Startable` interface). Containers declared as static fields will be shared between test 
methods. They will be started only once before any test method is executed and stopped after the last test method has 
executed. Containers declared as instance fields will be started and stopped for every test method.
  
**Note:** This extension has only be tested with sequential test execution. Using it with parallel test execution is 
unsupported and may have unintended side effects.
  
*Example:*
```java
@Testcontainers
class MyTestcontainersTests {
   
     // will be shared between test methods
    @Container
    private static final MySQLContainer MY_SQL_CONTAINER = new MySQLContainer();
    
     // will be started before and stopped after each test method
    @Container
    private PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer()
            .withDatabaseName("foo")
            .withUsername("foo")
            .withPassword("secret");
    @Test
    void test() {
        assertTrue(MY_SQL_CONTAINER.isRunning());
        assertTrue(postgresqlContainer.isRunning());
    }
}
```


## Examples

To use the Testcontainers extension annotate your test class with `@Testcontainers`.

### Restarted containers

To define a restarted container, define an instance field inside your test class and annotate it with
the `@Container` annotation.

```java
@Testcontainers
class SomeTest {

    @Container
    private MySQLContainer mySQLContainer = new MySQLContainer();

    @Test
    void someTestMethod() {
        String url = mySQLContainer.getJdbcUrl();

        // create a connection and run test as normal
    }

    @Nested
    class NestedTests {

        @Container
        private final PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer();

        void nestedTestMethod() {
            // top level container is restarted for nested methods
            String mySqlUrl = mySQLContainer.getJdbcUrl();
            
            // nested containers are only available inside their nested class
            String postgresUrl = postgreSQLContainer.getJdbcUrl();
        }
    }
}
```

### Shared containers

Shared containers are defined as static fields in a top level test class and have to be annotated with `@Container`.
Note that shared containers can't be declared inside nested test classes.
This is because nested test classes have to be defined non-static and can't therefore have static fields.

```java
@Testcontainers
class SomeTest {

    @Container
    private static final MySQLContainer MY_SQL_CONTAINER = new MySQLContainer();

    @Test
    void someTestMethod() {
        String url = MY_SQL_CONTAINER.getJdbcUrl();

        // create a connection and run test as normal
    }
}
```

### Singleton containers

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

## Limitations

Since this module has a dependency onto JUnit Jupiter and on Testcontainers core, which
has a dependency onto JUnit 4.x, projects using this module will end up with both, JUnit Jupiter
and JUnit 4.x in the test classpath.

This extension has only be tested with sequential test execution. Using it with parallel test execution is unsupported and may have unintended side effects.
