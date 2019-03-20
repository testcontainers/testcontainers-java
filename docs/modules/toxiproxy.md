# Toxiproxy Module

Testcontainers module for Shopify's [Toxiproxy](https://github.com/Shopify/toxiproxy). 
This TCP proxy can be used to simulate network failure conditions in between tests and containers.
 
[toxiproxy-java](https://github.com/trekawek/toxiproxy-java) is used as a client.

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

Then, to establish a connection via Toxiproxy, we obtain **Toxiproxy's** proxy host IP and port:

<!--codeinclude-->
[Obtaining proxied host and port](../../modules/toxiproxy/src/test/java/org/testcontainers/containers/ToxiproxyTest.java) inside_block:obtainProxiedHostAndPort
<!--/codeinclude-->

Code under test, or other containers, should connect to this proxied host IP and port.

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
testCompile "org.testcontainers:toxiproxy:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>toxiproxy</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
