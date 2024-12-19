# Azure Module

!!! note
This module is INCUBATING. While it is ready for use and operational in the current version of Testcontainers, it is possible that it may receive breaking changes in the future. See [our contributing guidelines](/contributing/#incubating-modules) for more information on our incubating modules policy.

Testcontainers module for the Microsoft Azure's [SDK](https://github.com/Azure/azure-sdk-for-java).

Currently, the module supports `Azurite` and `CosmosDB` emulators. In order to use them, you should use the following classes:

Class | Container Image
-|-
AzuriteContainer | [mcr.microsoft.com/azure-storage/azurite](https://github.com/microsoft/containerregistry)
CosmosDBEmulatorContainer | [mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator](https://github.com/microsoft/containerregistry)

## Usage example

### Azurite Storage Emulator

#### Using Blobs

Start Azurite Emulator during a test using Blob functionality:

<!--codeinclude-->
[Starting a Azurite Blob container](../../modules/azure/src/test/java/org/testcontainers/containers/AzuriteContainerTest.java) inside_block:blobEmulatorContainer
<!--/codeinclude-->

Get the connection string from the container:

<!--codeinclude-->
[Get connection string](../../modules/azure/src/test/java/org/testcontainers/containers/AzuriteContainerTest.java) inside_block:getBlobConnectionString
<!--/codeinclude-->

Build Azure Blob client:

<!--codeinclude-->
[Build Azure Blob Service client](../../modules/azure/src/test/java/org/testcontainers/containers/AzuriteContainerTest.java) inside_block:createBlobClient
<!--/codeinclude-->

Test against the Emulator:

<!--codeinclude-->
[Testing against Azurite container](../../modules/azure/src/test/java/org/testcontainers/containers/AzuriteContainerTest.java) inside_block:testWithBlobClient
<!--/codeinclude-->

#### Using Queues

Start Azurite Emulator during a test using Queue functionality:

<!--codeinclude-->
[Starting a Azurite Queue container](../../modules/azure/src/test/java/org/testcontainers/containers/AzuriteContainerTest.java) inside_block:queueEmulatorContainer
<!--/codeinclude-->

Get the connection string from the container:

<!--codeinclude-->
[Get connection string](../../modules/azure/src/test/java/org/testcontainers/containers/AzuriteContainerTest.java) inside_block:getQueueConnectionString
<!--/codeinclude-->

Build Azure Queue client:

<!--codeinclude-->
[Build Azure Queue Service client](../../modules/azure/src/test/java/org/testcontainers/containers/AzuriteContainerTest.java) inside_block:createQueueClient
<!--/codeinclude-->

Test against the Emulator:

<!--codeinclude-->
[Testing against Azurite container](../../modules/azure/src/test/java/org/testcontainers/containers/AzuriteContainerTest.java) inside_block:testWithQueueClient
<!--/codeinclude-->

#### Using Table

Start Azurite Emulator during a test using Table functionality:

<!--codeinclude-->
[Starting a Azurite Table container](../../modules/azure/src/test/java/org/testcontainers/containers/AzuriteContainerTest.java) inside_block:tableEmulatorContainer
<!--/codeinclude-->

Get the connection string from the container:

<!--codeinclude-->
[Get connection string](../../modules/azure/src/test/java/org/testcontainers/containers/AzuriteContainerTest.java) inside_block:getBlobConnectionString
<!--/codeinclude-->

Build Azure Table client:

<!--codeinclude-->
[Build Azure Table Service client](../../modules/azure/src/test/java/org/testcontainers/containers/AzuriteContainerTest.java) inside_block:createTableClient
<!--/codeinclude-->

Test against the Emulator:

<!--codeinclude-->
[Testing against Azurite container](../../modules/azure/src/test/java/org/testcontainers/containers/AzuriteContainerTest.java) inside_block:testWithTableClient
<!--/codeinclude-->

### CosmosDB

Start Azure CosmosDB Emulator during a test:

<!--codeinclude-->
[Starting a Azure CosmosDB Emulator container](../../modules/azure/src/test/java/org/testcontainers/containers/CosmosDBEmulatorContainerTest.java) inside_block:emulatorContainer
<!--/codeinclude-->

Prepare KeyStore to use for SSL.

<!--codeinclude-->
[Building KeyStore from certificate within container](../../modules/azure/src/test/java/org/testcontainers/containers/CosmosDBEmulatorContainerTest.java) inside_block:buildAndSaveNewKeyStore
<!--/codeinclude-->

Set system trust-store parameters to use already built KeyStore:

<!--codeinclude-->
[Set system trust-store parameters](../../modules/azure/src/test/java/org/testcontainers/containers/CosmosDBEmulatorContainerTest.java) inside_block:setSystemTrustStoreParameters
<!--/codeinclude-->

Build Azure CosmosDB client:

<!--codeinclude-->
[Build Azure CosmosDB client](../../modules/azure/src/test/java/org/testcontainers/containers/CosmosDBEmulatorContainerTest.java) inside_block:buildClient
<!--/codeinclude-->

Test against the Emulator:

<!--codeinclude-->
[Testing against Azure CosmosDB Emulator container](../../modules/azure/src/test/java/org/testcontainers/containers/CosmosDBEmulatorContainerTest.java) inside_block:testWithClientAgainstEmulatorContainer
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:azure:{{latest_version}}"
    ```

=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>azure</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

