# LDAP

Testcontainers module for [LLDAP](https://hub.docker.com/r/lldap/lldap).

## LLdapContainer's usage examples

You can start a LLDAP container instance from any Java application by using:

<!--codeinclude-->
[LLDAP container](../../modules/ldap/src/test/java/org/testcontainers/ldap/LLdapContainerTest.java) inside_block:container
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:ldap:{{latest_version}}"
    ```

=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>ldap</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
