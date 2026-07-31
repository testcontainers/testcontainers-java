# Hashicorp Vault Module

Testcontainers module for [Vault](https://github.com/hashicorp/vault). Vault is a tool for managing secrets. More information on Vault [here](https://www.vaultproject.io/).

## Usage example

You can start a Vault container instance from any Java application by using:

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

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:testcontainers-vault:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-vault</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

## License

See [LICENSE](https://raw.githubusercontent.com/testcontainers/testcontainers-java/main/modules/vault/LICENSE).

## Copyright

Copyright (c) 2017 Capital One Services, LLC and other authors.

See [AUTHORS](https://raw.githubusercontent.com/testcontainers/testcontainers-java/main/modules/vault/AUTHORS) for contributors.

