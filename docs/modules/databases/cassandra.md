# Cassandra Module

## Usage example

This example connects to the Cassandra cluster:

1. Define a container:
    <!--codeinclude-->
    [Container definition](../../../modules/cassandra/src/test/java/org/testcontainers/cassandra/CassandraDriver4Test.java) inside_block:container-definition
    <!--/codeinclude-->

2. Build a `CqlSession`:
    <!--codeinclude-->
    [Building CqlSession](../../../modules/cassandra/src/test/java/org/testcontainers/cassandra/CassandraDriver4Test.java) inside_block:cql-session
    <!--/codeinclude-->

!!!hint
    If you override the Cassandra configuration (using `CassandraContainer.withConfigurationOverride(String)`) to make
    the authentication mandatory (using `PasswordAuthenticator` for the property `authenticator`), it's strongly
    recommended to keep the default waiting strategy (`org.testcontainers.cassandra.wait.CassandraQueryWaitStrategy`) 
    in order to guarantee the init script will be executed once the Cassandra node is really ready to execute 
    authenticated queries, otherwise you may encounter an error like this one:
    ```
    AuthenticationFailed('Failed to authenticate to x.x.x.x: Error from server: code=0100 [Bad credentials]
    ```
    
    For example, assuming your custom `cassandra.yaml` configuration is located in a directory `cassandra-auth-required-configuration`:
    
    <!--codeinclude-->
    [Running init script with required authentication](../../../modules/cassandra/src/test/java/org/testcontainers/cassandra/CassandraContainerTest.java) inside_block:init-with-auth
    <!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:cassandra:{{latest_version}}"
    ```

=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>cassandra</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
