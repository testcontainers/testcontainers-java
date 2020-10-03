# Jqwik

[jqwik](https://jqwik.net/) is a Junit 5 test engine bringing Property-Based Testing (PBT) to the JVM. This module provides 
an API based on [jqwik lifecycle hooks](https://jqwik.net/docs/current/user-guide.html#lifecycle-hooks) to automatically
start and stop containers during a test run. It supports two lifecycles; namely a container per property falsification, 
or a container which is shared between all falsifications. At the moment, restarting containers between each property 
try is not supported.

In order to use this module in your tests, your project should be [set up for jqwik](https://jqwik.net/docs/current/user-guide.html#how-to-use)
and you should add the following dependency:

```groovy tab='Gradle'
testImplementation "org.testcontainers:junit-jqwik:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jqwik</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```

**Note:** This lifecycle hook has only been tested with sequential test execution. Using it with parallel test execution 
is unsupported and may result in unintended side effects.

## Testcontainers

Similar to the [Jupiter integration](./junit_5), the `@Testcontainers` annotation is the entry point of this module. If
the annotation is present on your class, jqwik will find all fields annotated with `@Container`. If any of these fields
is not `Startable`, the tests won't be run resulting in a failure. Shared containers are static fields which are started 
once before all properties and examples and stopped after all properties and examples. Restarted containers are instance 
fields which are started and stopped for every property or example.

jqwik starts shared containers before calling `@BeforeContainer` annotated methods and stops them after calling 
`@AfterContainer` annotated methods. Similar, restarted containers are started before calling `@BeforeProperty`and 
stopped after calling `@AfterProperty`.

<!--codeinclude-->
[Redis Backed Cache Integration Test](../examples/jqwik/examples/src/test/java/quickstart/RedisBackedCacheIntTest.java) block:withTestContainersAnnotation
<!--/codeinclude-->

The test above uses `@Testcontainers` with one redis `@Container`. It contains an `@Example` and a `@Property`. The 
redis container will be restarted between both. The assumption about redis is, that whatever key or value is used, 
the value should be able to be retrieved again by the key. jqwik generates keys and values and tries to falsify 
this assumption. By default, the property will be tried 1000 times.
 
## Groups

Like Jupiter, jqwik has a similar concept about grouping tests. A `@Group` is a means to improve the organization and
maintainability of your tests. It may contain own restarted containers which will be restarted for properties and
examples within a group but are not shared with subgroups.

<!--codeinclude-->
[Container of group not running in subgroup](../examples/jqwik/examples/src/test/java/groups/GroupedContainersTest.java) block:notSharedWithSubgroup
<!--/codeinclude-->

Example `grouped_container_should_be_running` would fail if it was not disabled. However, shared containers are running 
for all properties and all examples of every group.

<!--codeinclude-->
[Container of group not running in subgroup](../examples/jqwik/examples/src/test/java/groups/GroupedContainersTest.java) block:sharedWithSubgroup
<!--/codeinclude-->

### Restarted containers

To define a restarted container, define an instance field inside your test class and annotate it with
the `@Container` annotation.

<!--codeinclude-->
[Restarted Containers](../../modules/junit-jupiter/src/test/java/org/testcontainers/junit/jupiter/TestcontainersNestedRestartedContainerTests.java) inside_block:testClass
<!--/codeinclude-->


### Shared containers

Shared containers are defined as static fields in a top level test class and have to be annotated with `@Container`.
Note that shared containers can't be declared inside nested test classes.
This is because nested test classes have to be defined non-static and can't therefore have static fields.

<!--codeinclude-->
[Shared Container](../../modules/junit-jupiter/src/test/java/org/testcontainers/junit/jupiter/MixedLifecycleTests.java) lines:18-23,32-33,35-36
<!--/codeinclude-->

## Singleton containers

Note that the [singleton container pattern](manual_lifecycle_control.md#singleton-containers) is also an option when
using JUnit 5.

## Limitations

* Experimental API of jqwik
* Proximity
* Groups
* Not after each try
Since this module has a dependency onto JUnit Jupiter and on Testcontainers core, which
has a dependency onto JUnit 4.x, projects using this module will end up with both, JUnit Jupiter
and JUnit 4.x in the test classpath.

This extension has only be tested with sequential test execution. Using it with parallel test execution is unsupported and may have unintended side effects.
