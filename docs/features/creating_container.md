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
new GenericContainer("jboss/wildfly:9.0.1.Final")
```

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
