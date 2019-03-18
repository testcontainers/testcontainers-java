# Couchbase Module

<img src="https://cdn.worldvectorlogo.com/logos/couchbase.svg" width="300" />

Testcontainers module for Couchbase. [Couchbase](https://www.couchbase.com/) is a document oriented NoSQL database.

## Usage example

Running Couchbase as a stand-in in a test:

### Create your own bucket

```java
public class SomeTest {

    @Rule
    public CouchbaseContainer couchbase = new CouchbaseContainer()
             .withClusterAdmin("admin", "secret")
             .withNewBucket(DefaultBucketSettings.builder()
                        .enableFlush(true)
                        .name("bucket-name")
                        .password("secret")
                        .quota(100)
                        .type(BucketType.COUCHBASE)
                        .build());
    
    @Test
    public void someTestMethod() {
        Bucket bucket = couchbase.getCouchbaseCluster().openBucket("bucket-name");
        
        // ... interact with client as if using Couchbase normally
    }
}
```

### Use preconfigured default bucket

Bucket is cleared after each test

```java
public class SomeTest extends AbstractCouchbaseTest {

    @Test
    public void someTestMethod() {
        Bucket bucket = getBucket();
        
        // ... interact with client as if using Couchbase normally
    }
}
```

### Special consideration

Couchbase container is configured to use random available [ports](https://developer.couchbase.com/documentation/server/current/install/install-ports.html) for some ports only, as [Couchbase Java SDK](https://developer.couchbase.com/documentation/server/current/sdk/java/start-using-sdk.html) permit to configure only some ports: 

- **8091** : REST/HTTP traffic ([bootstrapHttpDirectPort](http://docs.couchbase.com/sdk-api/couchbase-java-client-2.4.6/com/couchbase/client/java/env/DefaultCouchbaseEnvironment.Builder.html#bootstrapCarrierDirectPort-int-))
- **18091** : REST/HTTP traffic with SSL ([bootstrapHttpSslPort](http://docs.couchbase.com/sdk-api/couchbase-java-client-2.4.6/com/couchbase/client/java/env/DefaultCouchbaseEnvironment.Builder.html#bootstrapCarrierSslPort-int-))
- **11210** : memcached ([bootstrapCarrierDirectPort](http://docs.couchbase.com/sdk-api/couchbase-java-client-2.4.6/com/couchbase/client/java/env/DefaultCouchbaseEnvironment.Builder.html#bootstrapCarrierDirectPort-int-))
- **11207** : memcached SSL ([bootstrapCarrierSslPort](http://docs.couchbase.com/sdk-api/couchbase-java-client-2.4.6/com/couchbase/client/java/env/DefaultCouchbaseEnvironment.Builder.html#bootstrapCarrierSslPort-int-))

All other ports cannot be changed by Java SDK, there are sadly fixed:

- **8092** : Queries, views, XDCR
- **8093** : REST/HTTP Query service
- **8094** : REST/HTTP Search Service
- **8095** : REST/HTTP Analytic service

So if you disable Query, Search and Analytic service, you can run multiple instance of this container, otherwise, you're stuck with one instance, for now.


## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testCompile "org.testcontainers:couchbase:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>couchbase</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
