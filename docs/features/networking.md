# Networking and communicating with containers

## Exposing container ports to the host

It is common to want to connect to a container from your test process, running on the test 'host' machine.
For example, you may be testing a class that needs to connect to a backend or data store container.

Generally, each required port needs to be explicitly exposed. For example, we can specify one or more ports as follows:

<!--codeinclude-->
[Exposing ports](../examples/junit4/generic/src/test/java/generic/MultiplePortsExposedTest.java) inside_block:rule
<!--/codeinclude-->

Note that this exposed port number is from the *perspective of the container*. 

*From the host's perspective* Testcontainers actually exposes this on a random free port.
This is by design, to avoid port collisions that may arise with locally running software or in between parallel test runs.

Because there is this layer of indirection, it is necessary to ask Testcontainers for the actual mapped port at runtime.
This can be done using the `getMappedPort` method, which takes the original (container) port as an argument:

<!--codeinclude-->
[Retrieving actual ports at runtime](../examples/junit4/generic/src/test/java/generic/MultiplePortsExposedTest.java) inside_block:fetchPortsByNumber
<!--/codeinclude-->

!!! warning
    Because the randomised port mapping happens during container startup, the container must be running at the time `getMappedPort` is called. 
    You may need to ensure that the startup order of components in your tests caters for this.

There is also a `getFirstMappedPort` method for convenience, for the fairly common scenario of a container that only exposes one port:

<!--codeinclude-->
[Retrieving the first mapped port](../examples/junit4/generic/src/test/java/generic/MultiplePortsExposedTest.java) inside_block:fetchFirstMappedPort
<!--/codeinclude-->

## Getting the container host

When running with a local Docker daemon, exposed ports will usually be reachable on `localhost`.
However, in some CI environments they may instead be reachable on a different host.

As such, Testcontainers provides a convenience method to obtain an address on which the container should be reachable from the host machine.

<!--codeinclude-->
[Getting the container host](../examples/junit4/generic/src/test/java/generic/MultiplePortsExposedTest.java) inside_block:getHostOnly
<!--/codeinclude-->

It is normally advisable to use `getHost` and `getMappedPort` together when constructing addresses - for example:

<!--codeinclude-->
[Getting the container host and mapped port](../examples/junit4/generic/src/test/java/generic/MultiplePortsExposedTest.java) inside_block:getHostAndMappedPort
<!--/codeinclude-->

!!! tip
    `getHost()` is a replacement for `getContainerIpAddress()` and returns the same result.
    `getContainerIpAddress()` is believed to be confusingly named, and will eventually be deprecated.

## Exposing host ports to the container

In some cases it is necessary to make a network connection from a container to a socket that is listening on the host machine.
Natively, Docker has limited support for this model across platforms.
Testcontainers, however, makes this possible.

In this example, assume that `localServerPort` is a port on our test host machine where a server (e.g. a web application) is running.

We need to tell Testcontainers to prepare to expose this port to containers:

<!--codeinclude-->
[Exposing the host port](../examples/junit4/generic/src/test/java/generic/HostPortExposedTest.java) inside_block:exposePort
<!--/codeinclude-->

!!! warning
    Note that the above command should be invoked _before_ containers are started, but _after_ the server on the host was started.
    
Having done so, we can now access this port from any containers that are launched.
From a container's perspective, the hostname will be `host.testcontainers.internal` and the port will be the same value as `localServerPort`.

For example, here we construct an HTTP URL for our local web application and tell a Selenium container to get a page from it:

<!--codeinclude-->
[Accessing the exposed host port from a container](../examples/junit4/generic/src/test/java/generic/HostPortExposedTest.java) inside_block:useHostExposedPort
<!--/codeinclude-->


## Advanced networking

Docker provides the ability for you to create custom networks and place containers on one or more networks. Then, communication can occur between networked containers without the need of exposing ports through the host. With Testcontainers, you can do this as well. 

!!! warning
    Note that Testcontainers currently only allows a container to be on a single network.

<!--codeinclude-->
[Creating custom networks](../../core/src/test/java/org/testcontainers/containers/NetworkTest.java) inside_block:useCustomNetwork
<!--/codeinclude-->
