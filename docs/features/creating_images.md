# Creating images on-the-fly

## Overview

In situations where there is no pre-existing Docker image, Testcontainers can create a new temporary image on-the-fly
from a Dockerfile. For example, when the component under test is the Docker image itself, or when an existing base
image needs to be customized for specific test(s).

Simply pass a configured instance of `ImageFromDockerfile` as a constructor parameter to `GenericContainer`.
Testcontainers will `docker build` a temporary container image, and will use it when creating the container.

## Dockerfile from String, file or classpath resource

`ImageFromDockerfile` accepts arbitrary files, strings or classpath resources to be used as files in the build context.
At least one of these needs to be a `Dockerfile`.
```java
@Rule
public GenericContainer dslContainer = new GenericContainer(
    new ImageFromDockerfile()
            .withFileFromString("folder/someFile.txt", "hello")
            .withFileFromClasspath("test.txt", "mappable-resource/test-resource.txt")
            .withFileFromClasspath("Dockerfile", "mappable-dockerfile/Dockerfile"))
```

The following methods may be used to provide the `Dockerfile` and any other required build context files:

* `withFileFromString(buildContextPath, content)`
* `withFileFromClasspath(buildContextPath, classpathPath)`
* `withFileFromPath(buildContextPath, filesystemPath)`
* `withFileFromFile(buildContextPath, filesystemFile)`

!!! info
    In older versions of Testcontainers (before 1.3.0) it was necessary to explicitly declare each file that needed to 
    be present in the Docker build context.
    This can be replaced with the following syntax:
    <!--codeinclude--> 
    [Passing an entire directory of files to the Dockerfile build context](../../core/src/test/java/org/testcontainers/images/builder/DockerfileBuildTest.java) inside_block:docsShowRecursiveFileInclusion
    <!--/codeinclude-->
    
    Where `RESOURCE_PATH` is the path to a directory containing a `Dockerfile` and any files that it needs to refer to.
    Doing this is equivalent to `docker build RESOURCE_PATH` on the command line.
    
    To mimic `docker build .`, `RESOURCE_PATH` would simply be set to `.` as well.

## Dockerfile DSL

If a static Dockerfile is not sufficient (e.g. your test needs to cover many variations that are best generated
programmatically), there is a DSL available to allow Dockerfiles to be defined in code. e.g.:
```java
new GenericContainer(
        new ImageFromDockerfile()
                .withDockerfileFromBuilder(builder ->
                        builder
                                .from("alpine:3.14")
                                .run("apk add --update nginx")
                                .cmd("nginx", "-g", "daemon off;")
                                .build()))
                .withExposedPorts(80);
```

See `ParameterizedDockerfileContainerTest` for a very basic example of using this in conjunction with JUnit
parameterized testing.

## Automatic deletion

Temporary container images will be automatically removed when the test JVM shuts down. If this is not desired and
the image should be retained between tests, pass a stable image name and `false` flag to the `ImageFromDockerfile`
constructor.

Retaining the image between tests will use Docker's image cache to accelerate subsequent test runs.

By default the no-args constructor will use an image name of the form `testcontainers/` + random string:

* `public ImageFromDockerfile()`
* `public ImageFromDockerfile(String dockerImageName)`
* `public ImageFromDockerfile(String dockerImageName, boolean deleteOnExit)`

## Alternative Dockerfiles

Normally Docker will automatically build an image from any `/Dockerfile` that it finds in the root of the build context.
To override this behaviour, use `.withDockerfilePath("./Name-Of-Other-Dockerfile")`.

## Build Args

[Build Args](https://docs.docker.com/engine/reference/builder/#arg) may be used to allow lightweight parameterization.

To specify build args, use `.withBuildArg("varname", "value")` or provide a `Map` of args using `.withBuildArgs(map)`.
