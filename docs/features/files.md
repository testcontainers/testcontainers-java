# Files and volumes

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

## Copying files to and from containers

Files can be copied into the container only before creation and files can be copied from the container only after container has started.

<!--codeinclude-->
[Copying files to and from container](../../core/src/test/java/org/testcontainers/junit/CopyFileToContainerTest.java) inside_block:copyToContainer
<!--/codeinclude-->
