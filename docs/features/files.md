# Files and volumes

## File mapping

It is possible to map a file or directory from your FileSystem into the container as a volume using `withFileSystemBind`:
```java
String pathToFile = String.format("%s/.aws", System.getProperty("user.home"))
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
