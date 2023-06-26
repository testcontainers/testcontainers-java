# K3s Module

!!! note
    This module is INCUBATING. While it is ready for use and operational in the current version of Testcontainers, it is possible that it may receive breaking changes in the future. See [our contributing guidelines](/contributing/#incubating-modules) for more information on our incubating modules policy.

Testcontainers module for Rancher's [K3s](https://rancher.com/products/k3s/) lightweight Kubernetes.
This module is intended to be used for testing components that interact with Kubernetes APIs - for example, operators.

## Usage example

Start a K3s server as follows:

<!--codeinclude-->
[Starting a K3S server](../../modules/k3s/src/test/java/org/testcontainers/k3s/Fabric8K3sContainerTest.java) inside_block:starting_k3s
<!--/codeinclude-->

### Connecting to the server

`K3sContainer` exposes a working Kubernetes client configuration, as a YAML String, via the `getKubeConfigYaml()` method.

This may be used with Kubernetes clients - e.g. for the [official Java client](connecting_with_k8sio) and 
[the Fabric8 Kubernetes client](https://github.com/fabric8io/kubernetes-client):

<!--codeinclude-->
[Official Java client](../../modules/k3s/src/test/java/org/testcontainers/k3s/OfficialClientK3sContainerTest.java) inside_block:connecting_with_k8sio
[Fabric8 Kubernetes client](../../modules/k3s/src/test/java/org/testcontainers/k3s/Fabric8K3sContainerTest.java) inside_block:connecting_with_fabric8
<!--/codeinclude-->

## Known limitations

!!! warning
    * K3sContainer runs as a privileged container and needs to be able to spawn its own containers. For these reasons,
    K3sContainer will not work in certain rootless Docker, Docker-in-Docker, or other environments where privileged
    containers are disallowed.

    * k3s containers may be unable to run on host machines where `/var/lib/docker` is on a BTRFS filesystem. See [k3s-io/k3s#4863](https://github.com/k3s-io/k3s/issues/4863) for an example.

    * You may experience PKIX exceptions when trying to use a configured Fabric8 client. This is down to newer distributions of k3s issuing elliptic curve keys.
    This can be fixed by adding [BouncyCastle PKI library](https://mvnrepository.com/artifact/org.bouncycastle/bcpkix-jdk15on) to your classpath.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:k3s:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>k3s</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
