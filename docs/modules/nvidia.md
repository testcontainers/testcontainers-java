# NVIDIA

Testcontainers module for [NVIDIA NIM](https://developer.nvidia.com/blog/a-simple-guide-to-deploying-generative-ai-with-nvidia-nim/)

## NVIDIA NIM's usage example

You can start an NVIDIA NIM container instance from any Java application by using:

<!--codeinclude-->
[NVIDIA NIM container](../../modules/nvidia/src/test/java/org/testcontainers/nvidia/NimContainerTest.java) inside_block:container
<!--/codeinclude-->

!!! note
    You need a NGC API Key. Read more https://docs.nvidia.com/nim/large-language-models/latest/getting-started.html#option-2-from-ngc

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:nvidia:{{latest_version}}"
    ```

=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>nvidia</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

