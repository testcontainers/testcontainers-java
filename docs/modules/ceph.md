# Ceph Module

!!! note
    This module is INCUBATING. While it is ready for use and operational in the current version of Testcontainers, it is possible that it may receive breaking changes in the future. See [our contributing guidelines](/contributing/#incubating-modules) for more information on our incubating modules policy.

Testcontainers module for [Ceph](https://ceph.io/).

## Ceph vs Localstack for simulating S3

One possible usage for the Ceph module is to take advantage of its S3 compatibility.

For simulating S3 you may wish to also consider the [Localstack](./localstack.md) module. 
Ceph and Localstack provide good compatibility with the S3 API, and we encourage you to evaluate both.

Localstack is likely to be a better choice if you require simulation of other AWS services along with S3. 

## Usage example

Creating a Ceph container:

<!--codeinclude-->
[Creating a Ceph container](../../modules/ceph/src/test/java/org/testcontainers/containers/ceph/CephContainerTest.java) inside_block:creating_container
<!--/codeinclude-->

Configuring an S3 Client to use Ceph:

<!--codeinclude-->
[Configuring an S3 Client to use Ceph](../../modules/ceph/src/test/java/org/testcontainers/containers/ceph/CephContainerTest.java) inside_block:setting_up_s3_client
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testCompile "org.testcontainers:ceph:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>ceph</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
