# JUnit 4

## `@Rule`/`@ClassRule` integration

**JUnit4 `@Rule`/`@ClassRule`**: This mode starts the container before your tests and tears it down afterwards.

Add a `@Rule` or `@ClassRule` annotated field to your test class, e.g.:

```java
public class SimpleMySQLTest {
    @Rule
    public MySQLContainer mysql = new MySQLContainer();
    
    // [...]
}
```


## Manually controlling container lifecycle

As an alternative, you can manually start the container in a `@BeforeAll`/`@BeforeEach` annotated method in your tests. Tear down will be done automatically on JVM exit, but you can of course also use an `@AfterAll`/`@AfterEach` annotated method to manually call the `close()` method on your container.

*Example of starting a container in a `@BeforeEach` annotated method:*

```java
class SimpleMySQLTest {
    private MySQLContainer mysql = new MySQLContainer();
    
    @BeforeEach
    void before() {
        mysql.start();
    }
    
    @AfterEach
    void after() {
        mysql.stop();
    }
    
    // [...]
}
```

