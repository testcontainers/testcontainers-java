# Hashicorp Vault Module

Testcontainers module for [Vault](https://github.com/hashicorp/vault). Vault is a tool for managing secrets. More information on Vault [here](https://www.vaultproject.io/).

## Usage example

Start Vault container as a `@ClassRule`:

<!--codeinclude-->
[Starting a Vault container](../../modules/vault/src/test/java/org/testcontainers/vault/VaultContainerTest.java) inside_block:vaultContainer
<!--/codeinclude-->

Use CLI to read data from Vault container:

<!--codeinclude-->
[Use CLI to read data](../../modules/vault/src/test/java/org/testcontainers/vault/VaultContainerTest.java) inside_block:readFirstSecretPathWithCli
<!--/codeinclude-->

Use Http API to read data from Vault container:

<!--codeinclude-->
[Use Http API to read data](../../modules/vault/src/test/java/org/testcontainers/vault/VaultContainerTest.java) inside_block:readFirstSecretPathOverHttpApi
<!--/codeinclude-->

Use client library to read data from Vault container:

<!--codeinclude-->
[Use library to read data](../../modules/vault/src/test/java/org/testcontainers/vault/VaultContainerTest.java) inside_block:readWithLibrary
<!--/codeinclude-->

[See full example.](https://github.com/testcontainers/testcontainers-java/blob/master/modules/vault/src/test/java/org/testcontainers/vault/VaultContainerTest.java)

## Why Vault in Junit tests?

With the increasing popularity of Vault and secret management, applications are now needing to source secrets from Vault.
This can prove challenging in the development phase without a running Vault instance readily on hand. This library 
aims to solve your apps integration testing with Vault. You can also use it to
test how your application behaves with Vault by writing different test scenarios in Junit.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:vault:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>vault</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

## License

See [LICENSE](https://raw.githubusercontent.com/testcontainers/testcontainers-java/main/modules/vault/LICENSE).

## Copyright

Copyright (c) 2017 Capital One Services, LLC and other authors.

See [AUTHORS](https://raw.githubusercontent.com/testcontainers/testcontainers-java/main/modules/vault/AUTHORS) for contributors.

