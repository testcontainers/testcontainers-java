# GCloud Module

Testcontainers module for the Google's [Cloud SDK](https://cloud.google.com/sdk/).

Currently, the module supports `datastore`, `firestore`, `pubsub` and `spanner` emulators. In order to use it, you should use the following classes:

* DatastoreEmulatorContainer
* FirestoreEmulatorContainer
* PubSubEmulatorContainer
* SpannerEmulatorContainer

## Usage example

Running GCloud as a stand-in for Google Firestore during a test:

```java
@Rule
public DatastoreEmulatorContainer emulator = new DatastoreEmulatorContainer();

@Test
public void someTestMethod() {
    DatastoreOptions options = DatastoreOptions.newBuilder()
    		.setHost(emulator.getContainerIpAddress() + ":" + emulator.getMappedPort(8081))
    		.setCredentials(NoCredentials.getInstance())
    		.setRetrySettings(ServiceOptions.getNoRetrySettings())
    		.build();
    Datastore datastore = options.getService();

    Key key = datastore.newKeyFactory().setKind("Task").newKey("sample");
    Entity entity = Entity.newBuilder(key).set("description", "my description").build();
    datastore.put(entity);

    assertThat(datastore.get(key).getString("description")).isEqualTo("my description");
```

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testCompile "org.testcontainers:gcloud:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>gcloud</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
