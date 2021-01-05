# NATS Module

## Usage example

This example connects to the NATS server and asserts the connection status of the client.

```java tab="JUnit Example"
public class NATSClientTest {

    @Rule
    public NATSContainer container = new NATSContainer();

    @Test
    public void test(){
        Connection connection = container.getConnection();
        assertThat(connection.getStatus(), equalTo(Connection.Status.CONNECTED));
    }

}
```

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testCompile "org.testcontainers:nats:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>nats</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
