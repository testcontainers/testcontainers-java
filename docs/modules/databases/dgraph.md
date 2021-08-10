# Dgraph Module

[Dgraph](https://dgraph.io/) is a native GraphQL graph database that is built to scale.
This module helps to run a Dgraph node or a multi-node cluster locally, to test how your project integrates with Dgraph.

## Usage example

You can create a single Dgraph node as easily as this:

```java
DockerImageName image = DockerImageName.parse("dgraph/dgraph:v21.03.1");
DgraphContainer<?> dgraphContainer = new DgraphContainer<>(image);
dgraphContainer.start();
```

Then you can use that Dgraph node instance for testing, for example connect to it via the [Dgraph4j Java client](https://github.com/dgraph-io/dgraph4j):

```java
import io.dgraph.DgraphClient;
import io.dgraph.DgraphGrpc;
import io.grpc.ManagedChannel;

ManagedChannel channel = ManagedChannelBuilder
    .forTarget(dgraphContainer.getGrpcUrl())
    .usePlaintext().build();
DgraphGrpc.DgraphStub stub = DgraphGrpc.newStub(channel);
DgraphClient client = new DgraphClient(stub);

dgraphClient.checkVersion().getTag();
```

## Configuration
A `DgraphContainer` runs [everything that is needed to start up a Dgraph cluster](https://dgraph.io/docs/deploy/overview/):
a [Dgraph Zero node](https://dgraph.io/docs/deploy/dgraph-zero/) and a [Dgraph Alpha node](https://dgraph.io/docs/deploy/dgraph-alpha/).

The [command line arguments](https://dgraph.io/docs/deploy/cli-command-reference/) of the Zero and Alpha processes can be fully controlled via the `withZeroArgument`/`withAlphaArgument` and `withZeroArgumentValues`/`withAlphaArgumentValues`:

Add a flag with a given value to the Zero command with `.withZeroArgument(@NonNull String argument, String value)`,
e.g. `.withZeroArgument("whitelist", "0.0.0.0/0")` adds `--whitelist "0.0.0.0/0"` to the Zero command:

    dgraph zero --whitelist "0.0.0.0/0"

Add a [super flag](https://dgraph.io/docs/deploy/config/#command-line-flags) value
to the Zero command with `.withZeroArgumentValues(@NonNull String argument, @NonNull String... values)`,
e.g. `.withZeroArgumentValues("security", "whitelist=0.0.0.0/0", "token=ABCD")` adds `--security "whitelist=0.0.0.0/0"` to the Zero command:

    dgraph zero --security "whitelist=0.0.0.0/0; token=ABCD"

Add flags and super flags to the Alpha command with `withAlphaArgument(@NonNull String argument, String value)` and `withAlphaArgumentValues(@NonNull String argument, @NonNull String... values)`.

You can check the actual Zero and Alpha commands with `getZeroCommand` and `getAlphaCommand`.

See the [Dgraph CLI Reference](https://dgraph.io/docs/deploy/cli-command-reference/) for all command line flags and super flags.

The Dgraph container exposes the Alpha's HTTP and gRPC ports. To get the actual port, call `getHttpPort` and `getGrpcPort`, respectively.
Methods `getHttpUrl` and `getGrpcUrl` return the full URL as a string.

## Cluster Example

The following example creates three Dgraph containers, each serving a Zero and an Alpha process. It then connects
all these processes to a single three-replicas cluster:

```java
DockerImageName DGRAPH_TEST_IMAGE = DockerImageName.parse("dgraph/dgraph:v21.03.1");

try (
    DgraphContainer<?> dgraphContainerOne = new DgraphContainer<>(DGRAPH_TEST_IMAGE);
    DgraphContainer<?> dgraphContainerTwo = new DgraphContainer<>(DGRAPH_TEST_IMAGE);
    DgraphContainer<?> dgraphContainerThree = new DgraphContainer<>(DGRAPH_TEST_IMAGE)
) {
    Network network = Network.newNetwork();

    dgraphContainerOne
        .withZeroArgument("my", "dgraph-one:5080")
        .withZeroArgumentValues("raft", "idx=1")
        .withZeroArgument("replicas", "3")
        .withNetworkAliases("dgraph-one")
        .withNetwork(network)
        .start();

    dgraphContainerTwo
        .dependsOn(dgraphContainerOne)
        .withZeroArgument("my", "dgraph-two:5080")
        .withZeroArgument("peer", "dgraph-one:5080")
        .withZeroArgumentValues("raft", "idx=2")
        .withZeroArgument("replicas", "3")
        .withNetworkAliases("dgraph-two")
        .withNetwork(network)
        .start();

    dgraphContainerThree
        .dependsOn(dgraphContainerOne)
        .withZeroArgument("my", "dgraph-three:5080")
        .withZeroArgument("peer", "dgraph-one:5080")
        .withZeroArgumentValues("raft", "idx=3")
        .withZeroArgument("replicas", "3")
        .withNetworkAliases("dgraph-three")
        .withNetwork(network)
        .start();

    // connect to the cluster (client will pick one of the alpha nodes)
    DgraphGrpc.DgraphStub[] stubs =
        Stream.of(dgraphContainerOne, dgraphContainerTwo, dgraphContainerThree)
            .map(DgraphContainer::getGrpcUrl)
            .map(ManagedChannelBuilder::forTarget)
            .map(ManagedChannelBuilder::usePlaintext)
            .map(ManagedChannelBuilder::build)
            .map(DgraphGrpc::newStub)
            .toArray(DgraphGrpc.DgraphStub[]::new);

    DgraphClient dgraphClient = new DgraphClient(stubs);

    dgraphClient.checkVersion().getTag();
}
```

Note: Testing with a Dgraph container does not require a multi-node Dgraph cluster.
This is just to exemplify a more complex configuration.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testImplementation "org.testcontainers:dgraph:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>dgraph</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
