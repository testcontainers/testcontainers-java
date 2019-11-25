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

As an alternative, you can manually start the container in a `@BeforeClass`/`@Before` annotated method in your tests. Tear down will be done automatically on JVM exit, but you can of course also use an `@AfterClass`/`@After` annotated method to manually call the `stop()` method on your container.

*Example of starting a container in a `@Before` annotated method:*

```java
class SimpleMySQLTest {
    private MySQLContainer mysql = new MySQLContainer();
    
    @Before
    void before() {
        mysql.start();
    }
    
    @After
    void after() {
        mysql.stop();
    }
    
    // [...]
}
```

## Singleton containers

Note that the [singleton container pattern](manual_lifecycle_control.md#singleton-containers) is also an option when
using JUnit 4.
