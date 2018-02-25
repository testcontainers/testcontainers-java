# TestContainers-Spock
[Spock](https://github.com/spockframework/spock) extension for [TestContainers](https://github.com/testcontainers/testcontainers-java) library, which allows to use Docker containers inside of Spock tests.

# Usage

## @Testcontainers class-annotation

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

## General TestContainers usage

See the [TestContainers documentation](https://www.testcontainers.org/) for more information about the underlying library.

# Attributions
The initial version of this project was heavily inspired by the excellent [JUnit5 docker extension](https://github.com/FaustXVI/junit5-docker) by [FaustXVI](https://github.com/FaustXVI).

# License
Copyright 2016 Kevin Wittek

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this project except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

See [LICENSE](LICENSE).
