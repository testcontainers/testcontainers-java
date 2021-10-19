# Neo4j Module

This module helps running [Neo4j](https://neo4j.com/download/) using Testcontainers.

Note that it's based on the [official Docker image](https://hub.docker.com/_/neo4j/) provided by Neo4j, Inc.

## Usage example

Declare your Testcontainer as a `@ClassRule` or `@Rule` in a JUnit 4 test or as static or member attribute of a JUnit 5 test annotated with `@Container` as you would with other Testcontainers.
You can either use call `getHttpUrl()` or `getBoltUrl()` on the Neo4j container.
`getHttpUrl()` gives you the HTTP-address of the transactional HTTP endpoint while `getBoltUrl()` is meant to be used with one of the [official Bolt drivers](https://neo4j.com/developer/language-guides/).
On the JVM you would most likely use the [Java driver](https://github.com/neo4j/neo4j-java-driver).

The following example uses the JUnit 5 extension `@Testcontainers` and demonstrates both the usage of the Java Driver and the REST endpoint:

```java tab="JUnit 5 example"
@Testcontainers
public class ExampleTest {

    @Container
    private static Neo4jContainer neo4jContainer = new Neo4jContainer()
        .withAdminPassword(null); // Disable password

    @Test
    void testSomethingUsingBolt() {

        // Retrieve the Bolt URL from the container
        String boltUrl = neo4jContainer.getBoltUrl();
        try (
            Driver driver = GraphDatabase.driver(boltUrl, AuthTokens.none());
            Session session = driver.session()
        ) {
            long one = session.run("RETURN 1", Collections.emptyMap()).next().get(0).asLong();
            assertThat(one, is(1L));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    void testSomethingUsingHttp() throws IOException {

        // Retrieve the HTTP URL from the container
        String httpUrl = neo4jContainer.getHttpUrl();

        URL url = new URL(httpUrl + "/db/data/transaction/commit");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);

        try (Writer out = new OutputStreamWriter(con.getOutputStream())) {
            out.write("{\"statements\":[{\"statement\":\"RETURN 1\"}]}");
            out.flush();
        }

        assertThat(con.getResponseCode(), is(HttpURLConnection.HTTP_OK));
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String expectedResponse = 
                "{\"results\":[{\"columns\":[\"1\"],\"data\":[{\"row\":[1],\"meta\":[null]}]}],\"errors\":[]}";
            String response = buffer.lines().collect(Collectors.joining("\n"));
            assertThat(response, is(expectedResponse));
        }
    }
}
```

You are not limited to Unit tests and can of course use an instance of the Neo4j Testcontainer in vanilla Java code as well.

## Additional features

### Disable authentication

Authentication can be disabled:

```java
@Testcontainers
public class ExampleTest {

    @Container
    Neo4jContainer neo4jContainer = new Neo4jContainer()
        .withoutAuthentication();
}
```

### Neo4j-Configuration

Neo4j's Docker image needs Neo4j configuration options in a dedicated format.
The container takes care of that and you can configure the database with standard options like the following:

```java
@Testcontainers
public class ExampleTest {

    @Container
    Neo4jContainer neo4jContainer = new Neo4jContainer()
        .withNeo4jConfig("dbms.security.procedures.unrestricted", "apoc.*,algo.*");
}
```

### Add custom plugins

Custom plugins, like APOC, can be copied over to the container from any classpath or host resource like this:

```java
@Testcontainers
public class ExampleTest {

    @Container
    Neo4jContainer neo4jContainer = new Neo4jContainer()
        .withPlugins(MountableFile.forClasspathResource("/apoc-3.5.0.1-all.jar"));
}
```

Whole directories work as well:

```java
@Testcontainers
public class ExampleTest {

    @Container
    Neo4jContainer neo4jContainer = new Neo4jContainer()
        .withPlugins(MountableFile.forClasspathResource("/my-plugins"));
}
```

### Start the container with a predefined database

If you have an existing database (`graph.db`) you want to work with, copy it over to the container like this:

```java
@Testcontainers
public class ExampleTest {

    @Container
    Neo4jContainer neo4jContainer = new Neo4jContainer()
        .withDatabase(MountableFile.forClasspathResource("/test-graph.db"));
}
```

## Choose your Neo4j license

If you need the Neo4j enterprise license, you can declare your Neo4j container like this:

```java
@Testcontainers
public class ExampleTest { 
    @ClassRule
    public static Neo4jContainer neo4jContainer = new Neo4jContainer()
        .withEnterpriseEdition();        
}
```

This creates a Testcontainer based on the Docker image build with the Enterprise version of Neo4j. 
The call to `withEnterpriseEdition` adds the required environment variable that you accepted the terms and condition of the enterprise version.
You accept those by adding a file named `container-license-acceptance.txt` to the root of your classpath containing the text `neo4j:3.5.0-enterprise` in one line.
You'll find more information about licensing Neo4j here: [About Neo4j Licenses](https://neo4j.com/licensing/).


## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testImplementation "org.testcontainers:neo4j:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>neo4j</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```

!!! hint
    Add the Neo4j Java driver if you plan to access the Testcontainer via Bolt:
    
    ```groovy tab='Gradle'
    compile "org.neo4j.driver:neo4j-java-driver:1.7.1"
    ```
    
    ```xml tab='Maven'
    <dependency>
        <groupId>org.neo4j.driver</groupId>
        <artifactId>neo4j-java-driver</artifactId>
        <version>1.7.1</version>
    </dependency>
    ```
    



