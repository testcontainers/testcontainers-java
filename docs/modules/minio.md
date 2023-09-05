# MinIO Containers

Test containers can be used to automatically instantiate and manage [MinIO](https://min.io) containers.

## Usage example

Create a `MinIOContainer` to use it in your tests:
```java
import org.testcontainers.containers.MinIOContainer;
MinIOContainer container = new MinIOContainer("minio/minio:latest");
```

The [MinIO Java client](https://min.io/docs/minio/linux/developers/java/API.html) can be configured with the container as such:
```java
import io.minio.MinioClient;
MinioClient minioClient = MinioClient
    .builder()
    .endpoint(container.getS3URL())
    .credentials(container.getUserName(), container.getPassword())
    .build();
```

If needed the username and password can be overridden with
```java
import org.testcontainers.containers.MinIOContainer;
MinIOContainer container = new MinIOContainer("minio/minio:latest")
        .withUserName("testuser")
        .withPassword("testpassword");
```

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
```groovy
testImplementation "org.testcontainers:minio:{{latest_version}}"
```

=== "Maven"
```xml
<dependency>
<groupId>org.testcontainers</groupId>
<artifactId>minio</artifactId>
<version>{{latest_version}}</version>
<scope>test</scope>
</dependency>
```
