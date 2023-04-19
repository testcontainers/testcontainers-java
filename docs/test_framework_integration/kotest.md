# Kotest

[Kotest](https://github.com/kotest/kotest) extension for [Testcontainers](https://github.com/testcontainers/testcontainers-java) library, which allows to use Docker containers inside of Kotest tests.

The source code is maintained at [Kotest Extensions TestContainers](https://github.com/kotest/kotest-extensions-testcontainers)
Issues and requests are maintained at the [Kotest Repository](https://github.com/kotest/kotest)

## Documentation

The full and updated documentation for Kotest integration with TestContainers can be found at [kotest.io](https://kotest.io/docs/extensions/test_containers.html)


### Dependencies

[<img src="https://img.shields.io/maven-central/v/io.kotest.extensions/kotest-extensions-testcontainers.svg?label=latest%20release"/>](https://search.maven.org/artifact/io.kotest.extensions/kotest-extensions-testcontainers)
[<img src="https://img.shields.io/nexus/s/https/oss.sonatype.org/io.kotest.extensions/kotest-extensions-testcontainers.svg?label=latest%20snapshot"/>](https://oss.sonatype.org/content/repositories/snapshots/io/kotest/extensions/kotest-extensions-testcontainers/)

To begin, add the following dependency to your Gradle build file.

```kotlin
testImplementation("io.kotest.extensions:kotest-extensions-testcontainers:VERSION")
```

Note: The group id is different (`io.kotest.extensions`) from the main kotest dependencies (`io.kotest`).

For Maven, you will need these dependencies:

```xml
<dependency>
    <groupId>io.kotest.extensions</groupId>
    <artifactId>kotest-extensions-testcontainers</artifactId>
    <version>${kotest.version}</version>
    <scope>test</scope>
</dependency>
```

### Example


```kotlin
class QueryDatastoreTest : FunSpec({
  val mysql = MySQLContainer<Nothing>("mysql:8.0.26").apply {
    startupAttempts = 1
    withUrlParam("connectionTimeZone", "Z")
    withUrlParam("zeroDateTimeBehavior", "convertToNull")
  }
  val ds = install(JdbcTestContainerExtension(mysql)) {
    poolName = "myconnectionpool"
    maximumPoolSize = 8
    idleTimeout = 10000
  }
  val datastore = PersonDatastore(ds)
  test("insert happy path") {
    datastore.insert(Person("sam", "Chicago"))
    datastore.insert(Person("jim", "Seattle"))
    datastore.findAll().shouldBe(listOf(
      Person("sam", "Chicago"),
      Person("jim", "Seattle"),
    ))
  }
})
```
