# Neo4j Module

This module helps to run [Neo4j](https://neo4j.com/download/) using Testcontainers.

Note that it's based on the [official Docker image](https://hub.docker.com/_/neo4j/) provided by Neo4j, Inc.

Even though the latest LTS version of Neo4j 4.4 is used in the examples of this documentation,
the Testcontainers integration supports also newer 5.x images of Neo4j.

## Usage example

Declare your Testcontainers as a `@ClassRule` or `@Rule` in a JUnit 4 test or as static or member attribute of a JUnit 5 test annotated with `@Container` as you would with other Testcontainers.
You can either use call `getBoltUrl()` or `getHttpUrl()` on the Neo4j container.
`getBoltUrl()` is meant to be used with one of the [official Bolt drivers](https://neo4j.com/developer/language-guides/) while `getHttpUrl()` gives you the HTTP-address of the transactional HTTP endpoint.
On the JVM you would most likely use the [Java driver](https://github.com/neo4j/neo4j-java-driver).

The following example uses the JUnit 5 extension `@Testcontainers` and demonstrates both the usage of the Java Driver and the REST endpoint:

<!--codeinclude-->
[JUnit 5 example](../../../examples/neo4j-container/src/test/java/org/testcontainers/containers/Neo4jExampleTest.java) inside_block:junitExample
<!--/codeinclude-->

You are not limited to Unit tests, and you can use an instance of the Neo4j Testcontainers in vanilla Java code as well.

## Additional features

### Custom password

A custom password can be provided:

<!--codeinclude-->
[Custom password](../../../modules/neo4j/src/test/java/org/testcontainers/containers/Neo4jContainerTest.java) inside_block:withAdminPassword
<!--/codeinclude-->

### Disable authentication

Authentication can be disabled:

<!--codeinclude-->
[Disable authentication](../../../modules/neo4j/src/test/java/org/testcontainers/containers/Neo4jContainerTest.java) inside_block:withoutAuthentication
<!--/codeinclude-->

### Random password

A random (`UUID`-random based) password can be set:

<!--codeinclude-->
[Random password](../../../modules/neo4j/src/test/java/org/testcontainers/containers/Neo4jContainerTest.java) inside_block:withRandomPassword
<!--/codeinclude-->

### Neo4j-Configuration

Neo4j's Docker image needs Neo4j configuration options in a dedicated format.
The container takes care of that, and you can configure the database with standard options like the following:

<!--codeinclude-->
[Neo4j configuration](../../../modules/neo4j/src/test/java/org/testcontainers/containers/Neo4jContainerTest.java) inside_block:neo4jConfiguration
<!--/codeinclude-->

### Add custom plugins

Custom plugins, like APOC, can be copied over to the container from any classpath or host resource like this:

<!--codeinclude-->
[Plugin jar](../../../modules/neo4j/src/test/java/org/testcontainers/containers/Neo4jContainerTest.java) inside_block:registerPluginsJar
<!--/codeinclude-->

Whole directories work as well:

<!--codeinclude-->
[Plugin folder](../../../modules/neo4j/src/test/java/org/testcontainers/containers/Neo4jContainerTest.java) inside_block:registerPluginsPath
<!--/codeinclude-->

### Add Neo4j Docker Labs plugins

Add any Neo4j Labs plugin from the [Neo4j Docker Labs plugin list](https://neo4j.com/docs/operations-manual/4.4/docker/operations/#docker-neo4jlabs-plugins).

!!! note
    At the moment only the plugins available from the list Neo4j Docker 4.4 are supported by type.
    If you want to register another supported Neo4j Labs plugin, you have to add it manually
    by using the method `withLabsPlugins(String... neo4jLabsPlugins)`.
    Please refer to the list of [supported Docker image plugins](https://neo4j.com/docs/operations-manual/current/docker/operations/#docker-neo4jlabs-plugins).

<!--codeinclude-->
[Configure Neo4j Labs Plugins](../../../modules/neo4j/src/test/java/org/testcontainers/containers/Neo4jContainerTest.java) inside_block:configureLabsPlugins
<!--/codeinclude-->


### Start the container with a predefined database

If you have an existing database (`graph.db`) you want to work with, copy it over to the container like this:

<!--codeinclude-->
[Copy database](../../../modules/neo4j/src/test/java/org/testcontainers/containers/Neo4jContainerTest.java) inside_block:copyDatabase
<!--/codeinclude-->

!!! note
    The `withDatabase` method will only work with Neo4j 3.5 and throw an exception if used in combination with a newer version.

## Choose your Neo4j license

If you need the Neo4j enterprise license, you can declare your Neo4j container like this:

<!--codeinclude-->
[Enterprise edition](../../../modules/neo4j/src/test/java/org/testcontainers/containers/Neo4jContainerTest.java) inside_block:enterpriseEdition
<!--/codeinclude-->

This creates a Testcontainers based on the Docker image build with the Enterprise version of Neo4j 4.4. 
The call to `withEnterpriseEdition` adds the required environment variable that you accepted the terms and condition of the enterprise version.
You accept those by adding a file named `container-license-acceptance.txt` to the root of your classpath containing the text `neo4j:4.4-enterprise` in one line.

If you are planning to run a newer Neo4j 5.x enterprise edition image, you have to manually define the proper enterprise image (e.g. `neo4j:5-enterprise`)
and set the environment variable `NEO4J_ACCEPT_LICENSE_AGREEMENT` by adding `.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")` to your container definition.

You'll find more information about licensing Neo4j here: [About Neo4j Licenses](https://neo4j.com/licensing/).


## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:neo4j:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>neo4j</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

!!! hint
    Add the Neo4j Java driver if you plan to access the Testcontainers via Bolt:
    
    === "Gradle"
        ```groovy
        compile "org.neo4j.driver:neo4j-java-driver:4.4.13"
        ```
    
    === "Maven"
        ```xml
        <dependency>
            <groupId>org.neo4j.driver</groupId>
            <artifactId>neo4j-java-driver</artifactId>
            <version>4.4.13</version>
        </dependency>
        ```
