# Container dependencies

It is common that a container `A` (say Redis CLI) needs to wait for a container `B` (say Redis) to be spawned before running.

In such a case you first declare your Redis container as you use to do:

<!--codeinclude--> 
[java](../examples/junit5/redis/src/test/java/generic/RedisContainerDependencyTest.java) inside_block:redisserver
<!--/codeinclude-->

And then declare your kafka container using testcontainers `dependsOn` keyword:

<!--codeinclude--> 
[java](../examples/junit5/redis/src/test/java/generic/RedisContainerDependencyTest.java) inside_block:rediscli
<!--/codeinclude-->

That way you can make sure that any container is started by testcontainers only when its dependencies are ready. 

For more information on waiting strategies refer to [`Waiting for containers to start or be ready`](startup_and_waits.md).