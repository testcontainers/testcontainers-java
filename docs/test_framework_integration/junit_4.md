# JUnit 4

This module provides an API that is based on the [JUnit 4](https://junit.org/junit4/) runner model.

## Using TestContainersRunner

Annotate your test class with `@RunWith(TestContainersRunner.class)` and add `@Container` or `@ClassContainer` annotations to your field containers, e.g.:

```java
@RunWith(TestContainersRunner.class)
public class SimpleMySQLTest {
    @Container
    public MySQLContainer mysql = new MySQLContainer();
    
    // [...]
}
```

Containers annotated with `@ClassContainer` will be shared between test methods. 
They will be started only once before any test method is executed and stopped after the last test method has executed. 
Containers annotated with `@Container` will be started and stopped for every test method.

## Using JUnit 4 Rules

Junit 4 only allows one runner per test class, so if your test class already uses [another runner](https://github.com/junit-team/junit4/wiki/Custom-runners) you should wrap your container instances with a `ContainerRule`:

```java
@RunWith(SpringJUnit4ClassRunner.class)
public class SomeTest {

    @Rule
    public ContainerRule<GenericContainer<?>> containerRule = new ContainerRule<>(
            new GenericContainer<>(TINY_IMAGE).withCommand("top")
    );

    @Test
    public void test() {
        assertThat(containerRule.get().isRunning()).isTrue();
    }
}
```

Static fields annotated with `@ClassRule` will be shared between test methods.
They will be started only once before any test method is executed and stopped after the last test method has executed.
Fields annotated with `@Rule` will be started and stopped for every test method.

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
