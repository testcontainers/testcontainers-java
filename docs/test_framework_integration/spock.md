# Spock

[Spock](https://github.com/spockframework/spock) extension for [Testcontainers](https://github.com/testcontainers/testcontainers-java) library, which allows to use Docker containers inside of Spock tests.

## Usage

### `@Testcontainers` class-annotation

Specifying the `@Testcontainers` annotation will instruct Spock to start and stop all testcontainers accordingly. This annotation 
can be mixed with Spock's `@Shared` annotation to indicate, that containers shouldn't be restarted between tests.

```groovy
@Testcontainers
class DatabaseTest extends Specification {

    @Shared
    PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer()
            .withDatabaseName("foo")
            .withUsername("foo")
            .withPassword("secret")

    def "database is accessible"() {

        given: "a jdbc connection"
        HikariConfig hikariConfig = new HikariConfig()
        hikariConfig.setJdbcUrl(postgreSQLContainer.jdbcUrl)
        hikariConfig.setUsername("foo")
        hikariConfig.setPassword("secret")
        HikariDataSource ds = new HikariDataSource(hikariConfig)

        when: "querying the database"
        Statement statement = ds.getConnection().createStatement()
        statement.execute("SELECT 1")
        ResultSet resultSet = statement.getResultSet()
        resultSet.next()

        then: "result is returned"
        int resultSetInt = resultSet.getInt(1)
        resultSetInt == 1
    }
}
```

## Adding Testcontainers Spock support to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testCompile "org.testcontainers:spock:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>spock</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```


## Attributions
The initial version of this project was heavily inspired by the excellent [JUnit5 docker extension](https://github.com/FaustXVI/junit5-docker) by [FaustXVI](https://github.com/FaustXVI).
