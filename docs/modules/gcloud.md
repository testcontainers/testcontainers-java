# GCloud Module

!!! note
    This module is INCUBATING. While it is ready for use and operational in the current version of Testcontainers, it is possible that it may receive breaking changes in the future. See [our contributing guidelines](/contributing/#incubating-modules) for more information on our incubating modules policy.

Testcontainers module for the Google Cloud Platform's [Cloud SDK](https://cloud.google.com/sdk/).

Currently, the module supports `BigQuery`, `Bigtable`, `Datastore`, `Firestore`, `Spanner`, and `Pub/Sub` emulators. In order to use it, you should use the following classes:

Class | Container Image
-|-
BigQueryEmulatorContainer | [ghcr.io/goccy/bigquery-emulator](https://ghcr.io/goccy/bigquery-emulator)
BigtableEmulatorContainer | [gcr.io/google.com/cloudsdktool/google-cloud-cli:emulators](https://gcr.io/google.com/cloudsdktool/google-cloud-cli)
DatastoreEmulatorContainer | [gcr.io/google.com/cloudsdktool/google-cloud-cli:emulators](https://gcr.io/google.com/cloudsdktool/google-cloud-cli)
FirestoreEmulatorContainer | [gcr.io/google.com/cloudsdktool/google-cloud-cli:emulators](https://gcr.io/google.com/cloudsdktool/google-cloud-cli)
SpannerEmulatorContainer | [gcr.io/cloud-spanner-emulator/emulator](https://gcr.io/cloud-spanner-emulator/emulator)
PubSubEmulatorContainer | [gcr.io/google.com/cloudsdktool/google-cloud-cli:emulators](https://gcr.io/google.com/cloudsdktool/google-cloud-cli)

## Usage example

### BigQuery

Start BigQuery Emulator during a test:

<!--codeinclude-->
[Starting a BigQuery Emulator container](../../modules/gcloud/src/test/java/org/testcontainers/gcloud/BigQueryEmulatorContainerTest.java) inside_block:emulatorContainer
<!--/codeinclude-->

<!--codeinclude-->
[Creating BigQuery Client](../../modules/gcloud/src/test/java/org/testcontainers/gcloud/BigQueryEmulatorContainerTest.java) inside_block:bigQueryClient
<!--/codeinclude-->

### Bigtable

Start Bigtable Emulator during a test:

<!--codeinclude-->
[Starting a Bigtable Emulator container](../../modules/gcloud/src/test/java/org/testcontainers/gcloud/BigtableEmulatorContainerTest.java) inside_block:emulatorContainer
<!--/codeinclude-->

Create a test Bigtable table in the Emulator:

<!--codeinclude-->
[Create a test table](../../modules/gcloud/src/test/java/org/testcontainers/gcloud/BigtableEmulatorContainerTest.java) inside_block:createTable
<!--/codeinclude-->

Test against the Emulator:

<!--codeinclude-->
[Testing with a Bigtable Emulator container](../../modules/gcloud/src/test/java/org/testcontainers/gcloud/BigtableEmulatorContainerTest.java) inside_block:testWithEmulatorContainer
<!--/codeinclude-->

### Datastore

Start Datastore Emulator during a test:

<!--codeinclude-->
[Starting a Datastore Emulator container](../../modules/gcloud/src/test/java/org/testcontainers/gcloud/DatastoreEmulatorContainerTest.java) inside_block:creatingDatastoreEmulatorContainer
<!--/codeinclude-->

And test against the Emulator:

<!--codeinclude-->
[Testing with a Datastore Emulator container](../../modules/gcloud/src/test/java/org/testcontainers/gcloud/DatastoreEmulatorContainerTest.java) inside_block:startingDatastoreEmulatorContainer
<!--/codeinclude-->

See more examples:

 * [Full sample code](https://github.com/testcontainers/testcontainers-java/tree/main/modules/gcloud/src/test/java/org/testcontainers/gcloud/DatastoreEmulatorContainerTest.java)
 * [With Spring Boot](https://github.com/saturnism/testcontainers-gcloud-examples/tree/main/springboot/datastore-example/src/test/java/com/example/springboot/datastore) 

### Firestore 

Start Firestore Emulator during a test:

<!--codeinclude-->
[Starting a Firestore Emulator container](../../modules/gcloud/src/test/java/org/testcontainers/gcloud/FirestoreEmulatorContainerTest.java) inside_block:emulatorContainer
<!--/codeinclude-->

And test against the Emulator:

<!--codeinclude-->
[Testing with a Firestore Emulator container](../../modules/gcloud/src/test/java/org/testcontainers/gcloud/FirestoreEmulatorContainerTest.java) inside_block:testWithEmulatorContainer
<!--/codeinclude-->

See more examples:

 * [Full sample code](https://github.com/testcontainers/testcontainers-java/tree/main/modules/gcloud/src/test/java/org/testcontainers/gcloud/FirestoreEmulatorContainerTest.java)
 * [With Spring Boot](https://github.com/saturnism/testcontainers-gcloud-examples/tree/main/springboot/firestore-example/src/test/java/com/example/springboot/firestore/FirestoreIntegrationTests.java)

### Spanner 

Start Spanner Emulator during a test:

<!--codeinclude-->
[Starting a Spanner Emulator container](../../modules/gcloud/src/test/java/org/testcontainers/gcloud/SpannerEmulatorContainerTest.java) inside_block:emulatorContainer
<!--/codeinclude-->

Create a test Spanner Instance in the Emulator:

<!--codeinclude-->
[Create a test Spanner instance](../../modules/gcloud/src/test/java/org/testcontainers/gcloud/SpannerEmulatorContainerTest.java) inside_block:createInstance
<!--/codeinclude-->

Create a test Database in the Emulator:

<!--codeinclude-->
[Creating a test Spanner database](../../modules/gcloud/src/test/java/org/testcontainers/gcloud/SpannerEmulatorContainerTest.java) inside_block:createDatabase
<!--/codeinclude-->

And test against the Emulator:

<!--codeinclude-->
[Testing with a Spanner Emulator container](../../modules/gcloud/src/test/java/org/testcontainers/gcloud/SpannerEmulatorContainerTest.java) inside_block:testWithEmulatorContainer
<!--/codeinclude-->

See more examples:

 * [Full sample code](https://github.com/testcontainers/testcontainers-java/tree/main/modules/gcloud/src/test/java/org/testcontainers/gcloud/SpannerEmulatorContainerTest.java)
 * [With Spring Boot](https://github.com/saturnism/testcontainers-gcloud-examples/tree/main/springboot/spanner-example/src/test/java/com/example/springboot/spanner/SpannerIntegrationTests.java)

### Pub/Sub 

Start Pub/Sub Emulator during a test:

<!--codeinclude-->
[Starting a Pub/Sub Emulator container](../../modules/gcloud/src/test/java/org/testcontainers/gcloud/PubSubEmulatorContainerTest.java) inside_block:emulatorContainer
<!--/codeinclude-->

Create a test Pub/Sub topic in the Emulator:

<!--codeinclude-->
[Create a test topic](../../modules/gcloud/src/test/java/org/testcontainers/gcloud/PubSubEmulatorContainerTest.java) inside_block:createTopic
<!--/codeinclude-->

Create a test Pub/Sub subscription in the Emulator:

<!--codeinclude-->
[Create a test subscription](../../modules/gcloud/src/test/java/org/testcontainers/gcloud/PubSubEmulatorContainerTest.java) inside_block:createSubscription
<!--/codeinclude-->

And test against the Emulator:

<!--codeinclude-->
[Testing with a Pub/Sub Emulator container](../../modules/gcloud/src/test/java/org/testcontainers/gcloud/PubSubEmulatorContainerTest.java) inside_block:testWithEmulatorContainer
<!--/codeinclude-->

See more examples:

 * [Full sample code](https://github.com/testcontainers/testcontainers-java/tree/main/modules/gcloud/src/test/java/org/testcontainers/gcloud/PubSubEmulatorContainerTest.java)
 * [With Spring Boot](https://github.com/saturnism/testcontainers-gcloud-examples/tree/main/springboot/pubsub-example/src/test/java/com/example/springboot/pubsub/PubSubIntegrationTests.java)

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:testcontainers-gcloud:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-gcloud</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
