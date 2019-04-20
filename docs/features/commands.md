# Executing commands

## Container startup command

By default the container will execute whatever command is specified in the image's Dockerfile. To override this, and specify a different command, use `withCommand`. For example:

<!--codeinclude-->
[Specifying a startup command](../examples/junit4/generic/src/test/java/generic/CommandsTest.java) inside_block:startupCommand
<!--/codeinclude-->

## Executing a command

Your test can execute a command inside a running container, similar to a `docker exec` call:

<!--codeinclude-->
[Executing a command inside a running container](../examples/junit4/generic/src/test/java/generic/ExecTest.java) inside_block:standaloneExec
<!--/codeinclude-->

This can be useful for software that has a command line administration tool. You can also get the output (stdout/stderr) and exit code from the command - for example:

<!--codeinclude-->
[Executing a command inside a running container and reading the result](../examples/junit4/generic/src/test/java/generic/ExecTest.java) inside_block:execReadingStdout
<!--/codeinclude-->

## Environment variables

To add environment variables to the container, use `withEnv`:
```java
new GenericContainer(...)
		.withEnv("API_TOKEN", "foo")
```
