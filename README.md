Testcontainers for Java

Testcontainers is an open-source Java library that makes it easy to write integration tests with real dependencies
java.testcontainers.org
. It lets you run “throwaway” Docker containers (for databases, web browsers, message brokers, etc.) during JUnit or Spock tests, without manual setup. You simply declare the test dependencies in code and Testcontainers handles starting and stopping the containers automatically
testcontainers.com
. This means there’s no need for fixed ports, environment configuration, or complex mocks – all you need is Docker
testcontainers.com
. Testcontainers works with popular JVM test frameworks (e.g. JUnit 4, JUnit 5/Jupiter, Spock)
java.testcontainers.org
.
Key Features

    Real containerized dependencies: Enables using actual services (e.g. MySQL, PostgreSQL, Oracle, Redis, Kafka, etc.) in tests, started from Docker images. For example, you can run a PostgreSQL or MySQL container to test your data layer against a real database
    java.testcontainers.org
    .

    Application integration tests: Run your application (or parts of it) with its dependencies (databases, message queues, caches) in isolated containers, ensuring tests use a clean, known environment
    java.testcontainers.org
    .

    UI/Acceptance tests: Supports containerized Selenium-compatible browsers (Chrome, Firefox, etc.) for UI tests, with each test getting a fresh browser instance (including optional video recording)
    java.testcontainers.org
    .

    Extensible modules: Many specialized modules are available (for databases, queues, cloud services, etc.). You can also use GenericContainer to define any Docker image as a test resource, or write custom containers. Testcontainers’ [GenericContainer] base class lets you create custom container tests as needed
    java.testcontainers.org
    .

Installation

Add the Testcontainers core library to your project’s test dependencies. For example, with Maven in pom.xml:

<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>testcontainers</artifactId>
  <version>1.21.3</version>
  <scope>test</scope>
</dependency>

java.testcontainers.org

Or with Gradle (Groovy DSL):

testImplementation "org.testcontainers:testcontainers:1.21.3"

Additionally, if you use JUnit 5 you should include the JUnit Jupiter integration:

testImplementation "org.testcontainers:junit-jupiter:1.21.3"

java.testcontainers.org

This pulls Testcontainers (and its transitive dependencies) from Maven Central. For managing multiple Testcontainers modules, you can also use the official Bill of Materials (testcontainers-bom) to avoid specifying versions on each dependency
java.testcontainers.org
.
Usage Example

Below is a simple JUnit 5 example. We annotate the test class with @Testcontainers and declare a @Container field. Testcontainers will start the container before the tests and stop it afterward automatically:

@Testcontainers
public class ExampleRedisTest {
    @Container
    public static GenericContainer<?> redis =
        new GenericContainer<>("redis:6-alpine")
            .withExposedPorts(6379);

    @Test
    public void testRedisConnection() {
        String address = redis.getHost();
        Integer port = redis.getFirstMappedPort();
        // ... use the Redis container in assertions ...
    }
}

This shows a JUnit 5 test that launches a Redis container for testing. (Adapted from the official quickstart guide
java.testcontainers.org
.) You could similarly use specific containers, e.g. PostgreSQLContainer, MySQLContainer, BrowserWebDriverContainer, etc. Testcontainers will pull the Docker image, start the container, wait until it’s ready, and then tear it down when tests complete.
Contributing

Testcontainers is a community-driven open-source project and welcomes contributions. See the CONTRIBUTING.md file for guidelines on reporting issues or submitting pull requests. (The repository is part of the GitHub Sponsors program, and sponsor-bounties may be available on issues
raw.githubusercontent.com
.) You can also find contribution tips and documentation guidelines in the project docs
raw.githubusercontent.com
raw.githubusercontent.com
.
License

This project is licensed under the MIT License (see the LICENSE file)
github.com
.
Copyright

© 2015–2021 Richard North and other authors.
MS SQL Server module © 2017–2021 G DATA Software AG and other authors.
HashiCorp Vault module © 2017–2021 Capital One Services, LLC and other authors.
See the CONTRIBUTORS list for all contributors
github.com
.

References: Official Testcontainers documentation and GitHub repo
java.testcontainers.org
testcontainers.com
java.testcontainers.org
github.com
.
