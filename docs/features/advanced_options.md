# Advanced options

## Container labels

To add a custom label to the container, use `withLabel`:

<!--codeinclude-->
[Adding a single label](../examples/junit4/generic/src/test/java/generic/ContainerLabelTest.java) inside_block:single_label
<!--/codeinclude-->

Additionally, multiple labels may be applied together from a map:

<!--codeinclude-->
[Adding multiple labels](../examples/junit4/generic/src/test/java/generic/ContainerLabelTest.java) inside_block:multiple_labels
<!--/codeinclude-->

## Image Pull Policy

By default, the container image is retrieved from the local Docker images cache.
This works well when running against a specific version, but for images with a static tag (i.e. 'latest') this may lead to a newer version not being pulled.

It is possible to specify an Image Pull Policy to determine at runtime whether an image should be pulled or not:

<!--codeinclude-->
[Setting image pull policy](../../core/src/test/java/org/testcontainers/images/ImagePullPolicyTest.java) inside_block:built_in_image_pull_policy
<!--/codeinclude-->

... or providing a function:

<!--codeinclude-->
[Custom image pull policy](../../core/src/test/java/org/testcontainers/images/ImagePullPolicyTest.java) inside_block:custom_image_pull_policy
<!--/codeinclude-->

## Customizing the container

It is possible to use the [`docker-java`](https://github.com/docker-java/docker-java) API directly to customize containers before creation. This is useful if there is a need to use advanced Docker features that are not exposed by the Testcontainers API. Any customizations you make using `withCreateContainerCmdModifier` will be applied _on top_ of the container definition that Testcontainers creates, but before it is created.

For example, this can be used to change the container hostname:

<!--codeinclude-->
[Using modifier to change hostname](../examples/junit4/generic/src/test/java/generic/CmdModifierTest.java) inside_block:hostname
<!--/codeinclude-->

... or modify container memory (see [this](https://fabiokung.com/2014/03/13/memory-inside-linux-containers/) if it does not appear to work):

<!--codeinclude-->
[Using modifier to change memory limits](../examples/junit4/generic/src/test/java/generic/CmdModifierTest.java) inside_block:memory
<!--/codeinclude-->

!!! note
    It is recommended to use this sparingly, and follow changes to the `docker-java` API if you choose to use this. 
    It is typically quite stable, though.

For what is possible, consult the [`docker-java CreateContainerCmd` source code](https://github.com/docker-java/docker-java/blob/3.2.1/docker-java-api/src/main/java/com/github/dockerjava/api/command/CreateContainerCmd.java).
