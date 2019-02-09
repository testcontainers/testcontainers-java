# JitPack (unreleased versions)

If you like to live on the bleeding edge, [jitpack.io](https://jitpack.io) can be used to obtain SNAPSHOT versions.
Use the following dependency description instead:

```xml
<dependency>
    <groupId>com.github.testcontainers.testcontainers-java</groupId>
    <artifactId>--artifact name--</artifactId>
    <version>-SNAPSHOT</version>
</dependency>
```

A specific git revision (such as `093a3a4628`) can be used as a fixed version instead. The JitPack maven repository must also be declared, e.g.:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

