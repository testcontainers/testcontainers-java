# Using Jib

[Jib](https://github.com/GoogleContainerTools/jib/tree/master/jib-core) is a library for building Docker images.
You can use it as an alternative to Testcontainers default `DockerfileBuilder`.

In order to use it along with Testcontainers, a `JibDockerClient` is needed.

<!--codeinclude-->
[DockerClient](../../core/src/test/java/org/testcontainers/containers/JibTest.java) inside_block:dockerClientInstance
<!--/codeinclude-->

The `JibDockerClient` cab be used in two forms

## Using Jib#from(String baseImageReference)

Jib provides different ways to create an image, read more [here](https://www.javadoc.io/static/com.google.cloud.tools/jib-core/0.23.0/com/google/cloud/tools/jib/api/Jib.html#from-java.lang.String-). 
This approach will use Jib's [RegistryClient](https://www.javadoc.io/static/com.google.cloud.tools/jib-core/0.23.0/com/google/cloud/tools/jib/registry/RegistryClient.html)
to download the `busybox` image and build `jib-hello-world` image, it will run by Testcontainers.

<!--codeinclude-->
[Jib#from(String)](../../core/src/test/java/org/testcontainers/containers/JibTest.java) inside_block:jibContainer1
<!--/codeinclude-->

## Using Jib#from(DockerClient dockerClient, DockerDaemonImage dockerDaemonImage)

The following approach relies on Testcontainers' `JibDockerClient` to download the `busybox` image and then build and run `jib-hello-world`.

<!--codeinclude-->
[Jib#from(DockerClient, DockerDaemonImage)](../../core/src/test/java/org/testcontainers/containers/JibTest.java) inside_block:jibContainer2
<!--/codeinclude-->

`JibDockerClient` can be used with `GenericContainer` as follows

<!--codeinclude-->
[GenericContainer with JibImage](../../core/src/test/java/org/testcontainers/containers/JibTest.java) inside_block:jibContainerUsage
<!--/codeinclude-->

!!! hint
Testcontainers library JAR will not automatically add a `jib-core` JAR to your project. Minimum version required is `com.google.cloud.tools:jib-core:0.22.0`.
