# Using Jib

[Jib](https://github.com/GoogleContainerTools/jib/tree/master/jib-core) is a library for building Docker images.

In order to use it along with Testcontainers, a DockerClient is needed.

<!--codeinclude-->
[DockerClient](../../core/src/test/java/org/testcontainers/containers/JibTest.java) inside_block:dockerClientInstance
<!--/codeinclude-->

The `JibDockerClient` should be used as follows

<!--codeinclude-->
[Jib#from(String)](../../core/src/test/java/org/testcontainers/containers/JibTest.java) inside_block:jibContainer1
[Jib#from(DockerClient, DockerDaemonImage)](../../core/src/test/java/org/testcontainers/containers/JibTest.java) inside_block:jibContainer2
<!--/codeinclude-->

!!! hint
Testcontainers library JAR will not automatically add a `jib-core` JAR to your project. You can start using `com.google.cloud.tools:jib-core:0.22.0`
