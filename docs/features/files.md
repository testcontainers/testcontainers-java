# Files and volumes

## Copying files

Files can be copied into the container before startup, or can be copied from the container after the container has started.

!!! note
    This is the recommended approach for portability cross-docker environments.

### Copying to a container before startup

<!--codeinclude-->
[Copying files using MountableFile](../../core/src/test/java/org/testcontainers/junit/CopyFileToContainerTest.java) inside_block:copyToContainer
<!--/codeinclude-->

Using `Transferable`, file content will be placed in the specified location.

<!--codeinclude-->
[Copying files using Transferable](../../core/src/test/java/org/testcontainers/containers/GenericContainerTest.java) inside_block:transferableFile
<!--/codeinclude-->

Setting file mode is also possible. 

<!--codeinclude-->
[Copying files using Transferable with file mode](../../core/src/test/java/org/testcontainers/containers/GenericContainerTest.java) inside_block:transferableWithFileMode
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

