# Creating a container

## Creating a generic container based on an image

Testcontainers' generic container support offers the most flexibility, and makes it easy to use virtually any container
images as temporary test dependencies. For example, if you might use it to test interactions with:

* NoSQL databases or other data stores (e.g. redis, elasticsearch, mongo)
* Web servers/proxies (e.g. nginx, apache)
* Log services (e.g. logstash, kibana)
* Other services developed by your team/organization which are already dockerized

With a generic container, you set the container image using a parameter to the rule constructor, e.g.:
```java
new GenericContainer(DockerImageName.parse("jboss/wildfly:9.0.1.Final"))
```

### Specifying an image

Many Container classes in Testcontainers have historically supported: 

* a no-args constructor - for example `new GenericContainer()` and `new ElasticsearchContainer()`. With these constructors, Testcontainers has traditionally used a default image name (including a fixed image tag/version). This has caused a conflict between the need to keep the defaults sane (i.e. up to date) and the need to avoid silently upgrading these dependencies along with new versions of Testcontainers. 
* a single string-argument constructor, which has taken either a version or an image name as a String. This has caused some ambiguity and confusion.

Since v1.15.0, both of these constructor types have been deprecated, for the reasons given above.

Instead, it is highly recommended that _all containers_ be constructed using a constructor that accepts a `DockerImageName` object.
The `DockerImageName` class is an unambiguous reference to a docker image.

It is suggested that developers treat `DockerImageName`s as you would any other potentially-constant value - consider defining a constant in your test codebase that matches the production version of the dependency you are using.

### Examples

A generic container rule can be used with any public docker image; for example:

<!--codeinclude--> 
[Creating a Redis container (JUnit 4)](../examples/junit4/generic/src/test/java/generic/ContainerCreationTest.java) inside_block:simple
<!--/codeinclude-->

Further options may be specified:

<!--codeinclude--> 
[Creating a container with more options (JUnit 4)](../examples/junit4/generic/src/test/java/generic/ContainerCreationTest.java) inside_block:withOptions
<!--/codeinclude-->

These containers, as `@ClassRule`s, will be started before any tests in the class run, and will be destroyed after all
tests have run.
