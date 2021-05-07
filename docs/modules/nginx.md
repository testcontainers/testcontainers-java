# Nginx Module

Nginx is a web server, reverse proxy and mail proxy and http cache.

## Usage example

The following example shows how to start Nginx.

<!--codeinclude-->
[Creating a Nginx container](../../modules/nginx/src/test/java/org/testcontainers/junit/SimpleNginxTest.java) inside_block:creatingContainer
<!--/codeinclude-->

How to add custom content to the Nginx server.

<!--codeinclude-->
[Creating the static content to serve](../../modules/nginx/src/test/java/org/testcontainers/junit/SimpleNginxTest.java) inside_block:addCustomContent
<!--/codeinclude-->

And how to query the Nginx server for the custom content added.

<!--codeinclude-->
[Creating the static content to serve](../../modules/nginx/src/test/java/org/testcontainers/junit/SimpleNginxTest.java) inside_block:getFromNginxServer
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testImplementation "org.testcontainers:nginx:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>nginx</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
