# Azure Module

!!! note
This module is INCUBATING. While it is ready for use and operational in the current version of Testcontainers, it is possible that it may receive breaking changes in the future. See [our contributing guidelines](/contributing/#incubating-modules) for more information on our incubating modules policy.

Testcontainers module for the Microsoft Azure's [SDK](https://github.com/Azure/azure-sdk-for-java).

Currently, the module supports `CosmosDB` emulator. In order to use it, you should use the following class:

Class | Container Image
-|-
CosmosDBEmulatorContainer | [mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator](https://github.com/microsoft/containerregistry)

## Usage example

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

```groovy tab='Gradle'
testImplementation "org.testcontainers:azure:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>azure</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```

