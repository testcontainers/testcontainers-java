# Files and volumes

## Copying files

!!! note
    This is the recommended approach for portability cross docker environments.

Files can be copied into the container before startup, or can be copied from the container after the container has started.

### Copying to a container before startup

<!--codeinclude-->
[Copying files to a container](../../core/src/test/java/org/testcontainers/junit/CopyFileToContainerTest.java) inside_block:copyToContainer
<!--/codeinclude-->

### Copying a file from a running container

<!--codeinclude-->
[Copying files from a container](../../core/src/test/java/org/testcontainers/junit/CopyFileToContainerTest.java) inside_block:copyFileFromContainer
<!--/codeinclude-->

## File mapping

It is possible to map a file or directory from your FileSystem into the container as a volume using `withFileSystemBind`:
```java
String pathToFile = System.getProperty("user.home") + "/.aws";
new GenericContainer(...)
        .withFileSystemBind(pathToFile, "/home/user/.aws", BindMode.READ_ONLY)
```

## Volume mapping

It is possible to map a file or directory **on the classpath** into the container as a volume using `withClasspathResourceMapping`:
```java
new GenericContainer(...)
        .withClasspathResourceMapping("redis.conf",
                                      "/etc/redis.conf",
                                      BindMode.READ_ONLY)
```

