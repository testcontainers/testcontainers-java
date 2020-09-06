# Imposter mock engine

[Imposter](https://github.com/outofcoffee/imposter/) is scriptable and extensible mock server for general OpenAPI (fka Swagger) specifications, REST APIs, and more.

Use Imposter to decouple your integration tests from the third party systems and take control of your dependencies.

Imposter lets you run standalone mocks in place of real systems, turning an OpenAPI/Swagger file into a live mock API for use by your application during tests.

## Imposter + Testcontainers

The `imposter` module for Testcontainers allows you to:

* Create mock endpoints from OpenAPI/Swagger v2 and OpenAPI v3 API specifications for your tests.
* Have the API endpoints defined in the specification automatically return response examples embedded in the specification.
* Use both static response files and script-driven responses, using status code, response files etc.

## Getting started

This example will show you how to use Testcontainers and Imposter to create a live mock from an OpenAPI/Swagger file and call it from a JUnit test.

### Prerequisites

Let's assume you are starting with a valid OpenAPI/Swagger 2.x or 3.x specification. See [this example](src/test/resources/specifications/petstore-simple.yaml) from this module's unit tests.

A few things to call out:

* We’ve defined the endpoint /pets as expecting an HTTP GET request
* We’ve said it will produce JSON responses
* One response is defined for the HTTP 200 case
* We’ve defined a basic data model in the definitions section — this is standard JSON Schema
* We’ve provided an example response — a JSON array of animals

> Tip: There’s much, much more to OpenAPI/Swagger than this. Check out [the specification](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md) for more info.

### Your JUnit test

In standard Testcontainers fashion, you can create your mock container using the builder pattern:

```java
public class MyThirdPartyMockTest {
    private static final Logger logger = LoggerFactory.getLogger(MyThirdPartyMockTest.class);
    
    @Rule
    public ImposterContainer<?> imposter;
    
    public MyThirdPartyMockTest() throws URISyntaxException {
        final Path specFile = Paths.get(MyThirdPartyMockTest.class.getResource("/specifications/petstore-simple.yaml").toURI());
        
        // spin up the mock engine using the given OpenAPI spec
        imposter = new ImposterContainer<>()
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .withSpecificationFile(specFile);
    }
```

#### What does this do?

When your JUnit tests run, Testcontainers will create and configure the Imposter mock server. The mock server will spin up an HTTP endpoint on a free port and will respond to requests for the APIs defined in your OpenAPI/Swagger file - that's the `/specifications/petstore-simple.yaml` path above.

For those APIs that have their `examples` defined, these will be used as the HTTP response body.

> See the [offical documentation on examples](https://swagger.io/docs/specification/adding-examples/) for more information.

#### What can I do with this?

The endpoint hosting your mock server is accessible via the `imposter.getBaseUrl()` method.

Normally this endpoint would be passed to your application code that expects to invoke a third party. Using Imposter, your application tests can invoke the mock endpoint instead, and your application receives a synthetic response instead of depending on a third party system.

#### Invoking the endpoint

We are going to write our test method to invoke the HTTP endpoint exposed by the mock server.

```java
    @Test
    public void testSpecExampleAsString() throws Exception {
        // this URL is the endpoint of the pets API defined in the spec
        final URI apiUrl = URI.create(imposter.getBaseUrl() + "/v1/pets");

        // invoke the mock endpoint
        final String response = someMethodThatReturnsTheHttpResponse(apiUrl);

        assertThat("An HTTP GET from the Imposter server returns the pets JSON array from the specification example",
            response,
            allOf(
                startsWith("["),
                containsString("Cat")
            )
        );
    
        // ... other assertions go here
    }
}
```

## Summary

You now have a simple mock server. Its endpoints are built automatically from your Swagger/OpenAPI specification. This means that if you add further endpoints, or make changes, Imposter will mock those too.

For brevity, we have not included the implementation of `someMethodThatReturnsTheHttpResponse(String)` as this is typically application specific and whatever HTTP client library you are using (e.g. OkHttp, Apache HttpClient etc.). The responsibility of this method is to invoke the `apiUrl` and return the HTTP response body as a String.

## What's next?

### Deserialising the model response

If you want to deserialise the response to a model object, you could do something like this:

```java
public class Pet {
    private Integer id;
    private String name;

    // ... getters/setters omitted for brevity
}
```

In your test:

```java
public class MyThirdPartyMockTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Rule
    public ImposterContainer<?> imposter;

    // ... same set-up as example above

    @Test
    public void testSpecExampleAsModel() throws Exception {
        final URI apiUrl = URI.create(imposter.getBaseUrl() + "/v1/pets");
        final String response = someMethodThatReturnsTheHttpResponse(apiUrl);
        
        // deserialise response to model
        final Pet[] pets = MAPPER.readValue(response, Pet[].class);
    
        assertThat("The first item in the pets array is a cat",
            pets[0].getName(),
            equalTo("Cat")
        );

        // ... other assertions go here
    }
}
```

### Dynamic/scripted responses

Imposter is simple to learn, but is also very customisable. If you outgrow returning static example responses from your specification, you can use scripts, written in JavaScript or Groovy, to control the response your mock returns.

For more information on scripted responses, check out [the documentation](https://github.com/outofcoffee/imposter/blob/master/docs/configuration.md#scripted-responses-advanced).
