# Using Jib

[Jib](https://github.com/GoogleContainerTools/jib/tree/master/jib-core) is a library for building Docker images.
You can use it as an alternative to Testcontainers default `DockerfileBuilder`.

<!--codeinclude-->
[GenericContainer with JibImage](../../core/src/test/java/org/testcontainers/containers/JibTest.java) inside_block:jibContainerUsage
<!--/codeinclude-->

!!! hint
The Testcontainers library JAR will not automatically add a `jib-core` JAR to your project. Minimum version required is `com.google.cloud.tools:jib-core:0.22.0`.
