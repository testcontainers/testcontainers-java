# Cyrus

!!! note
    This module is INCUBATING. While it is ready for use and operational in the current version of Testcontainers, it is possible that it may receive breaking changes in the future. See [our contributing guidelines](/contributing/#incubating-modules) for more information on our incubating modules policy.

Testcontainers module for [Cyrus Docker Test Server](https://github.com/cyrusimap/cyrus-docker-test-server).

## CyrusContainer usage example

You can start a Cyrus container instance from any Java application by using:

<!--codeinclude-->
[Create a CyrusContainer](../../modules/cyrus/src/test/java/org/testcontainers/cyrus/CyrusContainerTest.java) inside_block:container
<!--/codeinclude-->

The container exposes helpers for:

* protocol endpoints (`IMAP`, `POP3`, `HTTP/JMAP`, `LMTP`, `SIEVE`)
* strict management operations (`exportUser`, `importUser`, `deleteUser`)
* idempotent runtime helpers (`userExists`, `deleteUserIfExists`, `exportUserIfExists`, `createUserIfMissing`)
* startup user seeding (`withSeedEmptyUser`, `withSeedUser`, `withSeedUsers`) with deterministic replace behavior
* official image environment variables (`REFRESH`, `CYRUS_VERSION`, `DEFAULTDOMAIN`, `SERVERNAME`, `RELAYHOST`, `RELAYAUTH`)

## User Builder

Create a default empty user payload with `CyrusUser.builder(...)`:

<!--codeinclude-->
[Build a default Cyrus user](../../modules/cyrus/src/test/java/org/testcontainers/cyrus/CyrusUserTest.java) inside_block:userBuilder
<!--/codeinclude-->

## Startup Seeding

Seed users during container startup:

<!--codeinclude-->
[Seed users at startup](../../modules/cyrus/src/test/java/org/testcontainers/cyrus/CyrusContainerTest.java) inside_block:startupSeeding
<!--/codeinclude-->

When the same user is seeded multiple times, the last declaration wins.

## Runtime User Management

Use strict methods when missing users should fail fast, and idempotent methods when they should not:

* strict: `exportUser`, `importUser`, `deleteUser`
* idempotent: `userExists`, `deleteUserIfExists`, `exportUserIfExists`, `createUserIfMissing`

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:testcontainers-cyrus:{{latest_version}}"
    ```

=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-cyrus</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
