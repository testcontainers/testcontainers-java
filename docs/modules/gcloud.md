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

Running GCloud as a stand-in for Google Datastore during a test:

<!--codeinclude-->
[Creating a Datastore container](../../modules/gcloud/src/test/java/org/testcontainers/containers/DatastoreEmulatorContainerTest.java) inside_block:creatingDatastoreEmulatorContainer
<!--/codeinclude-->

And how to start it:

<!--codeinclude-->
[Starting a Datastore container](../../modules/gcloud/src/test/java/org/testcontainers/containers/DatastoreEmulatorContainerTest.java) inside_block:startingDatastoreEmulatorContainer
<!--/codeinclude-->

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
