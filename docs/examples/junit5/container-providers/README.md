# Container Providers Example

This example demonstrates the use of `@ContainerProvider` and `@ContainerConfig` annotations to share containers across multiple test classes.

## Problem

When you have multiple integration test classes that need the same container (e.g., a database), starting a new container for each test class is slow and wasteful. The traditional singleton pattern requires boilerplate code with static initializers.

## Solution

Container providers allow you to:
1. Define containers once using `@ContainerProvider`
2. Reference them by name using `@ContainerConfig`
3. Share containers across test classes automatically
4. Control lifecycle with scopes (CLASS or GLOBAL)

## Example Structure

```
src/test/java/
├── BaseIntegrationTest.java          # Base class with shared providers
├── UserServiceIntegrationTest.java   # Test class using shared database
├── OrderServiceIntegrationTest.java  # Another test class using same database
└── PaymentServiceIntegrationTest.java # Yet another test using same database
```

## Key Benefits

### Before (Manual Singleton Pattern)
```java
abstract class BaseIntegrationTest {
    static final PostgreSQLContainer<?> POSTGRES;
    
    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:14");
        POSTGRES.start();
    }
}

class UserServiceTest extends BaseIntegrationTest {
    @Test
    void test() {
        String jdbcUrl = POSTGRES.getJdbcUrl();
        // ...
    }
}
```

**Issues:**
- Boilerplate static initializer code
- Manual lifecycle management
- No type-safe parameter injection
- Hard to control when containers start/stop

### After (Container Providers)
```java
abstract class BaseIntegrationTest {
    @ContainerProvider(name = "database", scope = Scope.GLOBAL)
    public PostgreSQLContainer<?> createDatabase() {
        return new PostgreSQLContainer<>("postgres:14");
    }
}

@Testcontainers
class UserServiceTest extends BaseIntegrationTest {
    @Test
    @ContainerConfig(name = "database", injectAsParameter = true)
    void test(PostgreSQLContainer<?> db) {
        String jdbcUrl = db.getJdbcUrl();
        // ...
    }
}
```

**Benefits:**
- ✅ No boilerplate
- ✅ Automatic lifecycle management
- ✅ Type-safe parameter injection
- ✅ Declarative configuration
- ✅ Flexible scoping

## Running the Example

```bash
# Run all tests
./gradlew :junit-jupiter:test

# Run specific test
./gradlew :junit-jupiter:test --tests ContainerProviderBasicTests
```

## Performance Comparison

### Without Container Providers
- UserServiceTest: Start DB (5s) + Run tests (2s) = 7s
- OrderServiceTest: Start DB (5s) + Run tests (2s) = 7s
- PaymentServiceTest: Start DB (5s) + Run tests (2s) = 7s
- **Total: 21 seconds**

### With Container Providers (GLOBAL scope)
- Start DB once (5s)
- UserServiceTest: Run tests (2s)
- OrderServiceTest: Run tests (2s)
- PaymentServiceTest: Run tests (2s)
- **Total: 11 seconds (48% faster!)**

## Advanced Usage

### Multiple Containers
```java
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
void testDatabase(PostgreSQLContainer<?> db) { }

@Test
@ContainerConfig(name = "redis", injectAsParameter = true)
void testCache(GenericContainer<?> cache) { }
```

### Test Isolation
```java
@Test
@ContainerConfig(name = "database", needNewInstance = true)
void testWithFreshDatabase(PostgreSQLContainer<?> db) {
    // Gets a brand new database instance
    // Useful for tests that modify schema or data
}
```

### Mixing with Traditional @Container
```java
@Testcontainers
class MixedTest {
    @Container
    static final GenericContainer<?> TRADITIONAL = 
        new GenericContainer<>("httpd:2.4");
    
    @ContainerProvider(name = "modern", scope = Scope.CLASS)
    public GenericContainer<?> createModern() {
        return new GenericContainer<>("redis:6.2");
    }
    
    // Both approaches work together!
}
```

## Best Practices

1. **Use GLOBAL scope for expensive containers** (databases, message queues)
2. **Use CLASS scope for lightweight containers** that need isolation
3. **Use needNewInstance=true** for tests that modify container state
4. **Define providers in base classes** for cross-class sharing
5. **Use parameter injection** for type-safe container access

## Troubleshooting

### Provider not found
```
ExtensionConfigurationException: No container provider found with name 'myContainer'
```
**Solution:** Ensure the provider method is annotated with `@ContainerProvider(name = "myContainer")`

### Duplicate provider names
```
ExtensionConfigurationException: Duplicate container provider name 'database'
```
**Solution:** Each provider must have a unique name within the test class hierarchy

### Container returns null
```
ExtensionConfigurationException: Container provider method returned null
```
**Solution:** Provider methods must return a non-null Startable instance

## See Also

- [JUnit 5 Documentation](../../test_framework_integration/junit_5.md)
- [Manual Lifecycle Control](../../test_framework_integration/manual_lifecycle_control.md)
- [Singleton Containers](../../test_framework_integration/manual_lifecycle_control.md#singleton-containers)
