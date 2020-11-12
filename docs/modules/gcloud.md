# GCloud Module

!!! note
    This module is INCUBATING. While it is ready for use and operational in the current version of Testcontainers, it is possible that it may receive breaking changes in the future. See [our contributing guidelines](/contributing/#incubating-modules) for more information on our incubating modules policy.

Testcontainers module for the Google's [Cloud SDK](https://cloud.google.com/sdk/).

Currently, the module supports `Datastore`, `Firestore`, `Pub/Sub` and `Spanner` emulators. In order to use it, you should use the following classes:

* DatastoreEmulatorContainer
* FirestoreEmulatorContainer
* PubSubEmulatorContainer
* SpannerEmulatorContainer

## Usage example

### Datastore

Start Datastore Emulator during a test:

<!--codeinclude-->
[Starting a Datastore Emulator container](../../modules/gcloud/src/test/java/org/testcontainers/containers/DatastoreEmulatorContainerTest.java) inside_block:creatingDatastoreEmulatorContainer
<!--/codeinclude-->

And test against the Emulator:

<!--codeinclude-->
[Testing with a Datastore Emulator container](../../modules/gcloud/src/test/java/org/testcontainers/containers/DatastoreEmulatorContainerTest.java) inside_block:startingDatastoreEmulatorContainer
<!--/codeinclude-->

You can find more examples with [Spring Boot](https://github.com/saturnism/testcontainers-gcloud-examples/tree/main/springboot/datastore-example/src/test/java/com/example/springboot/datastore) and [without Spring Boot](https://github.com/saturnism/testcontainers-gcloud-examples/blob/main/noframeworks/datastore-example/src/test/java/com/example/noframeworks/datastore/DatastoreIntegrationTests.java).

### Firestore 

Start Firestore Emulator during a test:

<!--codeinclude-->
[Starting a Firestore Emulator container](../../modules/gcloud/src/test/java/org/testcontainers/containers/FirestoreEmulatorContainerTest.java) inside_block:emulatorContainer
<!--/codeinclude-->

And test against the Emulator:

<!--codeinclude-->
[Testing with a Firestore Emulator container](../../modules/gcloud/src/test/java/org/testcontainers/containers/FirestoreEmulatorContainerTest.java) inside_block:testWithEmulatorContainer
<!--/codeinclude-->

You can find more examples with [Spring Boot](https://github.com/saturnism/testcontainers-gcloud-examples/tree/main/springboot/firestore-example/src/test/java/com/example/springboot/firestore/FirestoreIntegrationTests.java) and [without Spring Boot](https://github.com/saturnism/testcontainers-gcloud-examples/blob/main/noframeworks/firestore-example/src/test/java/com/example/noframeworks/firestore/FirestoreIntegrationTests.java).

### Spanner 

Start Spanner Emulator during a test:

<!--codeinclude-->
[Starting a Spanner Emulator container](../../modules/gcloud/src/test/java/org/testcontainers/containers/SpannerEmulatorContainerTest.java) inside_block:emulatorContainer
<!--/codeinclude-->

Create a test Spanner Instance in the Emulator:

<!--codeinclude-->
[Create a test Spanner instance](../../modules/gcloud/src/test/java/org/testcontainers/containers/SpannerEmulatorContainerTest.java) inside_block:createInstance
<!--/codeinclude-->

Create a test Database in the Emulator:

<!--codeinclude-->
[Creating a test Spanner database](../../modules/gcloud/src/test/java/org/testcontainers/containers/SpannerEmulatorContainerTest.java) inside_block:createDatabase
<!--/codeinclude-->

And test against the Emulator:

<!--codeinclude-->
[Testing with a Firestore Emulator container](../../modules/gcloud/src/test/java/org/testcontainers/containers/SpannerEmulatorContainerTest.java) inside_block:testWithEmulatorContainer
<!--/codeinclude-->

You can find more examples with [Spring Boot](https://github.com/saturnism/testcontainers-gcloud-examples/tree/main/springboot/spanner-example/src/test/java/com/example/springboot/spanner/SpannerIntegrationTests.java) and [without Spring Boot](https://github.com/saturnism/testcontainers-gcloud-examples/blob/main/noframeworks/spanner-example/src/test/java/com/example/noframeworks/spanner/SpannerIntegrationTests.java).

### Pub/Sub 

Start Pub/Sub Emulator during a test:

<!--codeinclude-->
[Starting a Pub/Sub Emulator container](../../modules/gcloud/src/test/java/org/testcontainers/containers/PubSubEmulatorContainerTest.java) inside_block:emulatorContainer
<!--/codeinclude-->

Create a test Pub/Sub topic the Emulator:

<!--codeinclude-->
[Create a test topic](../../modules/gcloud/src/test/java/org/testcontainers/containers/PubSubEmulatorContainerTest.java) inside_block:createTopic
<!--/codeinclude-->

Create a test Pub/Sub subscription in the Emulator:

<!--codeinclude-->
[Create a test subscription](../../modules/gcloud/src/test/java/org/testcontainers/containers/PubSubEmulatorContainerTest.java) inside_block:createSubscription
<!--/codeinclude-->

And test against the Emulator:

<!--codeinclude-->
[Testing with a Pub/Sub Emulator container](../../modules/gcloud/src/test/java/org/testcontainers/containers/PubSubEmulatorContainerTest.java) inside_block:testWithEmulatorContainer
<!--/codeinclude-->

You can find more examples with [Spring Boot](https://github.com/saturnism/testcontainers-gcloud-examples/tree/main/springboot/pubsub-example/src/test/java/com/example/springboot/pubsub/PubSubIntegrationTests.java) and [without Spring Boot](https://github.com/saturnism/testcontainers-gcloud-examples/blob/main/noframeworks/pubsub-example/src/test/java/com/example/noframeworks/pubsub/PubSubIntegrationTests.java).

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testCompile "org.testcontainers:gcloud:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>gcloud</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
