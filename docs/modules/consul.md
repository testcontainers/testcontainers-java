# Hashicorp Consul Module

Testcontainers module for [Consul](https://github.com/hashicorp/consul). Consul is a tool for managing key value properties. More information on Consul [here](https://www.consul.io/).

## Usage example

Running Consul in your Junit tests is easily done with an @Rule or @ClassRule such as the following:

```java
public class SomeTest {

    @ClassRule
    public static ConsulContainer consulContainer = new ConsulContainer<>()
            .withPropertyInConsul("config/key", "value");
    
    @Test
    public void someTestMethod() {       
        //There are many integration clients for Consul so let's just define a general one here:
        final ConsulClient consulClient = new ConsulClient(consulContainer.getHost(), consulContainer.getFirstMappedPort());

        final Map<String, String> properties = new HashMap<>();
        properties.put("value", "world");
        properties.put("other_value", "another world");

        // Write operation
        properties.forEach((key, value) -> {
            Response<Boolean> writeResponse = consulClient.setKVValue(key, value);
            assertThat(writeResponse.getValue()).isTrue();
        });

        // Read operation
        properties.forEach((key, value) -> {
            Response<GetValue> readResponse = consulClient.getKVValue(key);
            assertThat(readResponse.getValue().getDecodedValue()).isEqualTo(value);
        });       
    }
```

## Why Consul in Junit tests?

With the increasing popularity of Consul and config externalization, applications are now needing to source properties from Consul.
This can prove challenging in the development phase without a running Consul instance readily on hand. This library 
aims to solve your apps integration testing with Consul. You can also use it to
test how your application behaves with Consul by writing different test scenarios in Junit.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testImplementation "org.testcontainers:consul:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>consul</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```

See [AUTHORS](https://raw.githubusercontent.com/testcontainers/testcontainers-java/master/modules/consul/AUTHORS) for contributors.

