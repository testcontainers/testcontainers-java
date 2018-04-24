# TestContainers MS SQL Server Module

Testcontainers module for the MS SQL Server database.

## Usage example

Running MS SQL Server as a stand-in for in a test:

```java
public class SomeTest {

    @Rule
    public MSSQLServerContainer mssqlserver = new MSSQLServerContainer();
    
    @Test
    public void someTestMethod() {
        String url = mssqlserver.getJdbcUrl();

        ... create a connection and run test as normal
```

> *Note:* Due to licencing restrictions you are required to accept an EULA for this container image. To indicate that you accept the MS SQL Server image EULA, Please place a file at the root of the classpath named `container-license-acceptance.txt`, e.g. at `src/test/resources/container-license-acceptance.txt`. This file should contain the line: `microsoft/mssql-server-linux:latest`

## Dependency information

Replace `VERSION` with the [latest version available on Maven Central](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.testcontainers%22).

### Maven

```
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mssqlserver</artifactId>
    <version>VERSION</version>
</dependency>
```

### Gradle

```
compile group: 'org.testcontainers', name: 'mssqlserver', version: 'VERSION'
```

## License

See [LICENSE](LICENSE).

## Copyright

Copyright (c) 2017 - 2019 G DATA Software AG and other authors.

See [AUTHORS](AUTHORS) for contributors.
