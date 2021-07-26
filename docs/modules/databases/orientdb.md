# OrientDB Module

!!! note
    This module is INCUBATING. While it is ready for use and operational in the current version of Testcontainers, it is possible that it may receive breaking changes in the future. See [our contributing guidelines](/contributing/#incubating-modules) for more information on our incubating modules policy.


This module helps running [OrientDB](https://orientdb.org/download) using Testcontainers.

Note that it's based on the [official Docker image](https://hub.docker.com/_/orientdb/) provided by OrientDB.

## Usage example

Declare your Testcontainer as a `@ClassRule` or `@Rule` in a JUnit 4 test or as static or member attribute of a JUnit 5 test annotated with `@Container` as you would with other Testcontainers.
You can call `getDbUrl()` OrientDB container and build the `ODatabaseSession` by your own, but a more useful `getSession()` method is provided.
On the JVM you would most likely use the [Java driver](https://github.com/).

The following example uses the JUnit 5 extension `@Testcontainers` and demonstrates both the usage of the Java Client:

```java tab="JUnit 5 example"
@Testcontainers
public class ExampleTest {

    @Container
    private static OrientDBContainer container = new OrientDBContainer();

    @Test
    void testDbCreation() {

        final ODatabaseSession session = container.getSession();

        session.command("CREATE CLASS Person EXTENDS V");
        session.command("INSERT INTO Person set name='john'");
        session.command("INSERT INTO Person set name='jane'");

        assertThat(session.query("SELECT FROM Person").stream()).hasSize(2);
    }

}
```

You are not limited to Unit tests and can of course use an instance of the OrientDB Testcontainer in vanilla Java code as well.


## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testImplementation "org.testcontainers:orientdb:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>orientdb</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```

!!! hint
    Add the OrientDB Java client if you plan to access the Testcontainer:
    
    ```groovy tab='Gradle'
    compile "com.orientechnologies:orientdb-client:3.0.24"
    ```
    
    ```xml tab='Maven'
    <dependency>
        <groupId>com.orientechnologies</groupId>
        <artifactId>orientdb-client</artifactId>
        <version>3.0.24</version>
    </dependency>
    ```
    



