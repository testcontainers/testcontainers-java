# Mockserver Module

Mock Server can be used to mock HTTP services by matching requests against user-defined expectations.

## Usage example

The following example shows how to set a simple expectation using the Java MockServerClient.

```java
public class SomeTest {

    @Rule
    public MockServerContainer container = new MockServerContainer();
    
    @Test
    public void someTestMethod() {
        new MockServerClient(container.getContainerIpAddress(), container.getServerPort())
                        .when(request()
                            .withMethod("GET")
                            .withPath("/person")
                            .withQueryStringParameter("name", "peter"))
                        .respond(response()
                            .withStatusCode(200)
                            .withBody("Peter the person!"));

        // ... making a get request to '/person?name=peter' will return the message "peter the person!" with a 200 status code
```

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testCompile "org.testcontainers:mockserver:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mockserver</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```

