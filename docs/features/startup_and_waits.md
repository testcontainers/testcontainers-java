# Waiting for containers to start or be ready

!!! info "Wait strategies vs Startup strategies"

    **Wait strategy:** is the container in a state that is useful for testing. This is generally approximated as 'can we talk to this container over the network'. However, there are quite a few variations and nuances.
    
    **Startup strategy:** did a container reach the desired running state. *Almost always* this just means 'wait until the container is running' - for a daemon process in a container this is the goal. Sometimes we need to wait until the container reaches a running state and then exits - this is the 'one shot startup' strategy, only used for cases where we need to run a one off command in a container but not a daemon.


## Wait Strategies

Ordinarily Testcontainers will wait for up to 60 seconds for the container's first mapped network port to start listening.

This simple measure provides a basic check whether a container is ready for use.

<!--codeinclude--> 
[Waiting for the first exposed port to start listening](../examples/src/test/java/generic/WaitStrategiesTest.java) inside_block:waitForNetworkListening
<!--/codeinclude-->

If the default 60s timeout is not sufficient, it can be altered with the `withStartupTimeout()` method.

If waiting for a listening TCP port is not sufficient to establish whether the container is ready, you can use the
`waitingFor()` method with other [`WaitStrategy`](http://static.javadoc.io/org.testcontainers/testcontainers/{{ latest_version }}/org/testcontainers/containers/wait/strategy/WaitStrategy.html) implementations as shown below.

### HTTP Wait strategy examples

You can choose to wait for an HTTP(S) endpoint to return a particular status code.

#### Waiting for 200 OK
<!--codeinclude--> 
[](../examples/src/test/java/generic/WaitStrategiesTest.java) inside_block:waitForSimpleHttp
<!--/codeinclude-->

Variations on the HTTP wait strategy are supported, including:

#### Waiting for multiple possible status codes
<!--codeinclude--> 
[](../examples/src/test/java/generic/WaitStrategiesTest.java) inside_block:waitForHttpWithMultipleStatusCodes
<!--/codeinclude-->

#### Waiting for a status code that matches a predicate
<!--codeinclude--> 
[Waiting for a status code that matches a predicate](../examples/src/test/java/generic/WaitStrategiesTest.java) inside_block:waitForHttpWithStatusCodePredicate
<!--/codeinclude-->

#### Using TLS
<!--codeinclude--> 
[](../examples/src/test/java/generic/WaitStrategiesTest.java) inside_block:waitForHttpWithTls
<!--/codeinclude-->

### Healthcheck Wait strategy examples

If the used image supports Docker's [Healthcheck](https://docs.docker.com/engine/reference/builder/#healthcheck) feature, you can directly leverage the `healthy` state of the container as your wait condition:

<!--codeinclude-->
[](../examples/src/test/java/generic/WaitStrategiesTest.java) inside_block:healthcheckWait
<!--/codeinclude-->

### Log output Wait Strategy

In some situations a container's log output is a simple way to determine if it is ready or not.
For example, we can wait for a `Ready' message in the container's logs as follows:

<!--codeinclude-->
[](../examples/src/test/java/generic/WaitStrategiesTest.java) inside_block:logMessageWait
<!--/codeinclude-->

### Other Wait Strategies

For further options, check out the [`Wait`](http://static.javadoc.io/org.testcontainers/testcontainers/{{ latest_version }}/org/testcontainers/containers/wait/strategy/Wait.html) convenience class, or the various subclasses of [`WaitStrategy`](http://static.javadoc.io/org.testcontainers/testcontainers/{{ latest_version }}/org/testcontainers/containers/wait/strategy/WaitStrategy.html). 

If none of these options meet your requirements, you can create your own subclass of 
[`AbstractWaitStrategy`](http://static.javadoc.io/org.testcontainers/testcontainers/{{ latest_version }}/org/testcontainers/containers/wait/strategy/AbstractWaitStrategy.html) with an 
appropriate wait mechanism in `waitUntilReady()`. 
The `GenericContainer.waitingFor()` method accepts any valid [`WaitStrategy`](http://static.javadoc.io/org.testcontainers/testcontainers/{{ latest_version }}/org/testcontainers/containers/wait/strategy/WaitStrategy.html).

