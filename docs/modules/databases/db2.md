# DB2 Module

!!! note
    This module is INCUBATING. While it is ready for use and operational in the current version of Testcontainers, it is possible that it may receive breaking changes in the future. See [our contributing guidelines](/contributing/#incubating-modules) for more information on our incubating modules policy.

See [Database containers](./index.md) for documentation and usage that is common to all relational database container types.

## Usage example

Running DB2 as a stand-in for in a test:

```java
public class SomeTest {

    @ClassRule
    public Db2Container db2 = new Db2Container()
        .acceptLicense();
    
    @Test
    public void someTestMethod() {
        String url = db2.getJdbcUrl();

        ... create a connection and run test as normal
    }
```

!!! warning "EULA Acceptance"
    Due to licencing restrictions you are required to accept an EULA for this container image. To indicate that you accept the DB2 image EULA, call the `acceptLicense()` method, or place a file at the root of the classpath named `container-license-acceptance.txt`, e.g. at `src/test/resources/container-license-acceptance.txt`. This file should contain the line: `ibmcom/db2:11.5.0.0a` (or, if you are overriding the docker image name/tag, update accordingly).
    
    Please see the [`ibmcom/db2` image documentation](https://hub.docker.com/r/ibmcom/db2) for a link to the EULA document.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testImplementation "org.testcontainers:db2:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>db2</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```

!!! hint
    Adding this Testcontainers library JAR will not automatically add a database driver JAR to your project. You should ensure that your project also has a suitable database driver as a dependency.
