# Files and volumes

## Volume mapping

It is possible to map a file or directory **on the classpath** into the container as a volume using `withClasspathResourceMapping`:
```java
new GenericContainer(...)
        .withClasspathResourceMapping("redis.conf",
                                      "/etc/redis.conf",
                                      BindMode.READ_ONLY)
```

## Copying files to and from containers

Files can be copied into the container before startup, or can be copied from the container after the container has started.

### Copying to a container before startup

<!--codeinclude-->
[Copying files to a container](../../core/src/test/java/org/testcontainers/junit/CopyFileToContainerTest.java) inside_block:copyToContainer
<!--/codeinclude-->

### Copying a file from a running container

<!--codeinclude-->
[Copying files from a container](../../core/src/test/java/org/testcontainers/junit/CopyFileToContainerTest.java) inside_block:copyFromContainer
<!--/codeinclude-->
