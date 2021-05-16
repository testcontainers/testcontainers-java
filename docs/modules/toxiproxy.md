# Toxiproxy Module

Testcontainers module for Shopify's [Toxiproxy](https://github.com/Shopify/toxiproxy). 
This TCP proxy can be used to simulate network failure conditions.

You can simulate network failures:

* between Java code and containers, ideal for testing resilience features of client code
* between containers, for testing resilience and emergent behaviour of multi-container systems
* if desired, between Java code/containers and external resources (non-Dockerized!), for scenarios where not all dependencies can be/have been dockerized

Testcontainers Toxiproxy support allows resilience features to be easily verified as part of isolated dev/CI testing. This allows earlier testing of resilience features, and broader sets of failure conditions to be covered.
 
## Usage example

A Toxiproxy container can be placed in between test code and a container, or in between containers.
In either scenario, it is necessary to create a `ToxiproxyContainer` instance on the same Docker network, as follows:

<!--codeinclude-->
[Creating a Toxiproxy container](../../modules/toxiproxy/src/test/java/org/testcontainers/containers/ToxiproxyTest.java) inside_block:creatingProxy
<!--/codeinclude-->

Next, it is necessary to instruct Toxiproxy to start proxying connections. 
Each `ToxiproxyContainer` can proxy to many target containers if necessary.

We do this as follows:

<!--codeinclude-->
[Starting proxying connections to a target container](../../modules/toxiproxy/src/test/java/org/testcontainers/containers/ToxiproxyTest.java) inside_block:obtainProxyObject
<!--/codeinclude-->

To establish a connection from the test code (on the host machine) to the target container via Toxiproxy, we obtain **Toxiproxy's** proxy host IP and port:

<!--codeinclude-->
[Obtaining proxied host and port for connections from the host machine](../../modules/toxiproxy/src/test/java/org/testcontainers/containers/ToxiproxyTest.java) inside_block:obtainProxiedHostAndPortForHostMachine
<!--/codeinclude-->

Code under test should connect to this proxied host IP and port.

To establish a connection from a different container on the same network to the target container via Toxiproxy, we use **Toxiproxy's** network alias and original port:

<!--codeinclude-->
[Obtaining proxied host and port for connections from a different container](../../modules/toxiproxy/src/test/java/org/testcontainers/containers/ToxiproxyTest.java) inside_block:obtainProxiedHostAndPortForDifferentContainer
<!--/codeinclude-->

Other containers should connect to this proxied host and port.

Having done this, it is possible to trigger failure conditions ('Toxics') through the `proxy.toxics()` object:

* `bandwidth` - Limit a connection to a maximum number of kilobytes per second.
* `latency` - Add a delay to all data going through the proxy. The delay is equal to `latency +/- jitter`.
* `slicer` - Slices TCP data up into small bits, optionally adding a delay between each sliced "packet".
* `slowClose` - Delay the TCP socket from closing until `delay` milliseconds has elapsed.
* `timeout` - Stops all data from getting through, and closes the connection after `timeout`. If `timeout` is `0`, the connection won't close, and data will be delayed until the toxic is removed.
* `limitData` - Closes connection when transmitted data exceeded limit.

Please see the [Toxiproxy documentation](https://github.com/Shopify/toxiproxy#toxics) for full details on the available Toxics.

As one example, we can introduce latency and random jitter to proxied connections as follows:

<!--codeinclude-->
[Adding latency to a connection](../../modules/toxiproxy/src/test/java/org/testcontainers/containers/ToxiproxyTest.java) inside_block:addingLatency
<!--/codeinclude-->

Additionally we can disable the proxy to simulate a complete interruption to the network connection:

<!--codeinclude-->
[Cutting a connection](../../modules/toxiproxy/src/test/java/org/testcontainers/containers/ToxiproxyTest.java) inside_block:disableProxy
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testImplementation "org.testcontainers:toxiproxy:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>toxiproxy</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```

## Acknowledgements

This module was inspired by a [hotels.com blog post](https://medium.com/hotels-com-technology/i-dont-know-about-resilience-testing-and-so-can-you-b3c59d80012d).

[toxiproxy-java](https://github.com/trekawek/toxiproxy-java) is used to help control failure conditions.
