# Testcontainers Java
Testcontainers Java is a library that supports JUnit tests by providing lightweight, throwaway instances of common databases, Selenium web browsers, or anything else that can run in a Docker container. This is a Gradle-based Java project with over 60 container modules and comprehensive testing infrastructure.

Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.

## Working Effectively

### Prerequisites
- Java 17+ (JDK 17 is the primary development JDK)
- Docker Engine running and accessible
- Gradle 8.14+ (via ./gradlew wrapper)

### Bootstrap, Build, and Test the Repository
- **Compilation**: `./gradlew :testcontainers:compileJava` -- Core module compilation takes ~30 seconds
- **Core Module Build**: `./gradlew :testcontainers:compileTestJava` -- Test compilation takes ~30 seconds  
- **Single Test Run**: `./gradlew :testcontainers:test --tests "*GenericContainerTest*" --no-daemon` -- Takes ~1 minute. NEVER CANCEL. Set timeout to 5+ minutes.
- **Example Project Test**: `./gradlew :docs:examples:junit5:redis:test --no-daemon` -- Takes ~25 seconds. NEVER CANCEL. Set timeout to 3+ minutes.
- **Module-Specific Build**: `./gradlew :testcontainers-postgresql:check --no-daemon` (example for postgresql module) -- Each module build takes 2-15 minutes. NEVER CANCEL. Set timeout to 30+ minutes.
- **Full Build**: `./gradlew check` -- Takes 45+ minutes. NEVER CANCEL. Set timeout to 90+ minutes.

### Known Build Issues and Workarounds
- **Spotless Formatter Network Issue**: The `spotlessCheck` task may fail with `java.net.UnknownHostException: groovy.jfrog.io`. This is a network connectivity issue in some environments. Workaround: Use compilation and test tasks that avoid spotless, or run with `--continue` to skip failing formatter tasks.
- **Build Cache Failures**: Remote build cache may be unavailable (`Loading entry from 'https://ge.testcontainers.org/cache/...' failed`). This is expected and doesn't affect build success.

### Format and Lint
- **Apply Code Formatting**: `./gradlew spotlessApply` -- Requires Node.js to be installed. May fail due to network issues (see Known Issues above).
- **Check Formatting**: `./gradlew spotlessCheck` -- May fail due to network connectivity to groovy.jfrog.io
- **Checkstyle**: Automatically runs as part of `check` task using config/checkstyle/checkstyle.xml

## Validation and Testing

### Manual Testing Scenarios
- **ALWAYS run end-to-end scenarios** after making changes to core functionality
- **Test Container Lifecycle**: Run any test that starts/stops containers (e.g., RedisBackedCacheIntTest) to verify Docker integration
- **Module-Specific Testing**: When modifying a specific module, run `./gradlew :testcontainers-postgresql:test` (example for postgresql module) to verify module functionality
- **Example Verification**: Test examples in `docs/examples/` to ensure documentation code works: `./gradlew :docs:examples:junit5:redis:test`

### Test Execution Patterns
- Tests use Docker containers heavily - ensure Docker daemon is running
- Most tests follow JUnit 5 + @Testcontainers pattern
- Container modules are in `modules/` directory, each with their own test suites
- Core functionality tests are in `core/src/test/java/org/testcontainers/`

### CI Build Requirements
- **ALWAYS run** `./gradlew check` before committing (when network allows)
- **Required Java versions**: Tests run on Java 17 and 21 in CI
- **Docker dependency**: All tests require Docker runtime

## Repository Structure and Navigation

### Key Directories
- **`core/`** - Main testcontainers library code (:testcontainers project)
- **`modules/`** - 60+ container-specific modules (e.g., :testcontainers-postgresql, :testcontainers-kafka, :testcontainers-mysql)
- **`docs/examples/`** - Working code examples for documentation
- **`examples/`** - Standalone example projects (separate from docs)
- **`buildSrc/`** - Custom Gradle build logic
- **`.github/workflows/`** - CI/CD pipeline definitions

### Core Module Structure
```
core/src/main/java/org/testcontainers/
├── containers/           # Container implementations
├── images/              # Image building and management
├── utility/             # Utility classes
└── DockerClientFactory.java  # Docker client management
```

### Module Structure (e.g., modules/postgresql/)
```
modules/postgresql/src/main/java/org/testcontainers/containers/
└── PostgreSQLContainer.java  # Container-specific implementations
```

### Important Files
- **`build.gradle`** - Root build configuration
- **`settings.gradle`** - Multi-project build settings
- **`gradle.properties`** - Build properties (testcontainers.version=1.21.3)
- **`CONTRIBUTING.md`** - Comprehensive contribution guidelines

## Common Tasks and Patterns

### Adding a New Module
- Follow checklist in `docs/contributing.md`
- Create new directory in `modules/[name]/`
- Add to `settings.gradle` automatically via `file('modules').eachDir` pattern
- Implement container class extending appropriate base class
- Add comprehensive tests following existing patterns

### Running Specific Tests
- **Single test class**: `./gradlew :testcontainers:test --tests "ClassNameTest"`
- **Package tests**: `./gradlew :testcontainers:test --tests "org.testcontainers.containers.*"`
- **Module tests**: `./gradlew :testcontainers-postgresql:test` (example for postgresql module)

### Working with Examples
- Examples use project dependencies on core testcontainers
- Run from root: `./gradlew :docs:examples:junit5:redis:test`
- Examples demonstrate real-world usage patterns

### Container Development Patterns
- Extend `GenericContainer<SELF>` for basic containers
- Extend `JdbcDatabaseContainer` for database containers  
- Use `@Testcontainers` + `@Container` annotations in tests
- Implement proper wait strategies for container readiness

## Docker and Environment Requirements

### Docker Environment
- Docker must be running and accessible to current user
- Tests will pull container images as needed (e.g., redis:6-alpine)
- Some tests require specific Docker features (e.g., networking, volumes)

### Performance Considerations
- **First run**: Takes longer due to Docker image pulls
- **Subsequent runs**: Faster with local image cache
- **Parallel execution**: Gradle parallel execution is disabled by default (`org.gradle.parallel=false`)

## Troubleshooting Common Issues

### Build Failures
- **"groovy.jfrog.io: No address associated with hostname"**: Network issue with spotless formatter, use compilation tasks instead
- **Docker daemon not accessible**: Ensure Docker is running and user has permissions
- **Out of memory**: Increase heap with `export GRADLE_OPTS="-Xmx4g"`

### Test Failures
- **Container startup timeout**: May indicate Docker resource constraints
- **Port binding issues**: Usually resolves on retry, or indicates port conflicts
- **Image pull failures**: Check Docker Hub connectivity

### Development Environment
- **Java version**: Use Java 17+ (project requires Java 17 toolchain)
- **IDE setup**: Project uses Lombok - ensure annotation processing is enabled
- **Git hooks**: Auto-applied via `captainHook` plugin for formatting

Always test your changes with Docker containers running to ensure full integration testing.