# Reusable containers (ALPHA)

!!! warning "Reusable containers are still an alpha feature"

    Reusable containers are still an **ALPHA** feature. It has not been finished yet. The current state can be found in
    the pull request [#1781](https://github.com/testcontainers/testcontainers-java/pull/1781).

In contrast to [singleton containers](../test_framework_integration/manual_lifecycle_control.md/#singleton-containers)
reusable containers survive the end of the tests. This may speed up consecutive test runs.

## What reusable containers can do

 * Reusable containers support hashing. This is used to detect configuration changes. // TODO: please check if this is correct
 * JDBC url support.
 * Check for overrides like `containerIsCreated`. // TODO: I am not quite sure if I understand what this means and what it is used for.
 * Per environment enablement. This feature has to be enabled twice, via property and local testcontainers configuration.

## What reusable cannot do

Hence this is an alpha feature it is not yet fully implemented.

 * Locking for parallel tests are not supported yet. Tests with the same hash will use the same container.
 * Containers will not be deleted automatically. The user has to stop and delete them manually. (Check `docker ps` after
   running a test with reusable containers.)
 * There is no clean up mechanism when the configuration changes. This will start a new container.
 * *Started* successfully marker to avoid race conditions. // TODO: I am not sure what the difference to the overrides part with `containerIsCreated` is.
 * It is not possible to start the containers via JUnit 4's or JUnit 5's `@Rule` and `@Container` annotations. // TODO: is this correct? As far as I understand the config changes and the container will not be reused.

## Prerequisites

There is one essential prerequisite: `testcontainers.reuse.enable=true` needs to be set in your local 
testcontainers.properties file. The location depends on the machine and can be found in the 
[configuration file location](./configuration/#configuration-file-location) documentation.

If tests are run without the configuration but the `withReuse(true)` was set on the container, there will be a warning:

> Reuse was requested but the environment does not support the reuse of containers

## Starting a reusable containers

In order to create a test tha uses reusable containers you need to create it somewhere you can start it manually. For
example in JUnit 4 you can create a static instance of a container and use a `@BeforeClass` annotated set-up method to
instantiate and start the container.

<!--codeinclude-->
[Using reusable containers](../examples/junit4/generic/src/test/java/generic/ReusableContainersTest.java) inside_block:single_label
<!--/codeinclude-->

After running this test once, you can see that the container survived the end of the test execution with `docker ps`.
You will find a `nginx` container there. When running the test another time the container will be reused this avoiding
start-up time.
Another benefit is, if you have multiple tests in different test files which require the same container regardless of
it's current state, it can be reused by initializing it the same as before.

## Examples

Beside generic containers you can also use the specialized containers. Here are some examples.

// THIS EXAMPLES HAS BEEN COPIED AND ADJUSTED FOR THE USE HERE. @Sergei, I hope your okay with this, those examples
// show diverse use of the current state of reusable containers.

### Example usage with container objects

<!--codeinclude-->
[Test using the Kafka container](../examples/junit4/generic/src/test/java/generic/ReusableKafkaContainerTest.java) inside_block:single_label
<!--/codeinclude-->

Here we unset the network that was implicitly created by KafkaContainer (but not used in this case), because otherwise
the network id will always be random and affect the hash. Kafka is an exceptional case (to be fixed, left for backward 
compatibility) and most of other containers do not set the network implicitly.

### Example usage with JDBC URLs

<!--codeinclude-->
[Test that uses JDBC urls](../examples/junit4/generic/src/test/java/generic/ReusableContainerWithJdbcUrlsTest.java) inside_block:single_label
<!--/codeinclude-->

## Ongoing work

There is still work to do to become a production-ready feature. Some of those additions are: 

 * Networks
 * Multi container setups
 * Automatic deletion of old containers (TTL)
 * Cleanup for new container configurations
 * Locking for parallel tests
