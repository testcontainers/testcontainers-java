# MS SQL Server Module

Testcontainers module for [MS SQL Server](https://mcr.microsoft.com/en-us/artifact/mar/mssql/server/)

## Usage example

You can start a MS SQL Server container instance from any Java application by using:

<!--codeinclude-->
[Container definition](../../../modules/mssqlserver/src/test/java/org/testcontainers/junit/mssqlserver/SimpleMSSQLServerTest.java) inside_block:container
<!--/codeinclude-->

!!! warning "EULA Acceptance"
    Due to licencing restrictions you are required to accept an EULA for this container image. To indicate that you accept the MS SQL Server image EULA, call the `acceptLicense()` method, or place a file at the root of the classpath named `container-license-acceptance.txt`, e.g. at `src/test/resources/container-license-acceptance.txt`. This file should contain the line: `mcr.microsoft.com/mssql/server:2017-CU12` (or, if you are overriding the docker image name/tag, update accordingly).
    
    Please see the [`microsoft-mssql-server` image documentation](https://hub.docker.com/_/microsoft-mssql-server#environment-variables) for a link to the EULA document.

See [Database containers](./index.md) for documentation and usage that is common to all relational database container types.

### Testcontainers JDBC URL

`jdbc:tc:sqlserver:2017-CU12:///databasename`

See [JDBC](./jdbc.md) for documentation.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:mssqlserver:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>mssqlserver</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```


!!! hint
    Adding this Testcontainers library JAR will not automatically add a database driver JAR to your project. You should ensure that your project also has a suitable database driver as a dependency.

## License

See [LICENSE](https://raw.githubusercontent.com/testcontainers/testcontainers-java/main/modules/mssqlserver/LICENSE).

## Copyright

Copyright (c) 2017 - 2019 G DATA Software AG and other authors.

See [AUTHORS](https://raw.githubusercontent.com/testcontainers/testcontainers-java/main/modules/mssqlserver/AUTHORS) for contributors.
