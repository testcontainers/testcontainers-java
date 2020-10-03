# Jqwik

!!! note
    This module is INCUBATING. While it is ready for use and operational in the current version of Testcontainers, it is 
    possible that it may receive breaking changes in the future. See [our contributing guidelines](/contributing/#incubating-modules) 
    for more information on our incubating modules policy.


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

Similar to the [Jupiter integration](../junit_5), the `@Testcontainers` annotation is the entry point of this module. If
the annotation is present on your class, jqwik will find all fields annotated with `@Container`. If any of these fields
is not `Startable`, the tests won't be run resulting in a failure. Shared containers are static fields which are started 
once before all properties and examples and stopped after all properties and examples. Restarted containers are instance 
fields which are started and stopped for every property or example.

jqwik starts shared containers before calling `@BeforeContainer` annotated methods and stops them after calling 
`@AfterContainer` annotated methods. Similar, restarted containers are started before calling `@BeforeProperty`and 
stopped after calling `@AfterProperty`.

<!--codeinclude-->
[Redis Backed Cache Integration Test](../examples/jqwik/examples/src/test/java/quickstart/RedisBackedCacheIntTest.java) inside_block:withTestContainersAnnotation
<!--/codeinclude-->

The test above uses `@Testcontainers` with two redis `@Container`s. `sharedContainer` will run for the whole test while 
`redis` will be restarted between the `@Example` and the `@Property`. The assumption about redis is, that whatever key
or value is used, the value should be able to be retrieved again by the key. jqwik generates keys and values and tries 
to falsify this assumption. By default, the property will be tried 1000 times.
 
## Groups

Like Jupiter, jqwik has a similar concept about grouping tests. A `@Group` is a means to improve the organization and
maintainability of your tests. It may contain own restarted containers which will be restarted for properties and
examples within a group but are not shared with subgroups.

<!--codeinclude-->
[Container not shared with subgroups](../examples/jqwik/examples/src/test/java/groups/GroupedContainersTest.java) inside_block:notSharedWithSubgroup
<!--/codeinclude-->

Example `grouped_container_should_be_running` would fail if it was not disabled. However, shared containers are running 
for all properties and all examples of every group.

<!--codeinclude-->
[Container shared with subgroups](../examples/jqwik/examples/src/test/java/groups/GroupedContainersTest.java) inside_block:sharedWithSubgroup
<!--/codeinclude-->

## Singleton containers

Note that the [singleton container pattern](manual_lifecycle_control.md#singleton-containers) is also an option when
using JUnit 5.

## Limitations

jqwik follows the guidelines of the [api guardian project](https://github.com/apiguardian-team/apiguardian). Lifecycle 
hooks in jqwik are an experimental feature which means that parts of it can change in the future without prior notice. 
This could render this module useless for future jqwik versions. At the time of implementation, jqwik version 1.3.6 was 
used. 

Lifecycle hooks use proximity to determine when a hook should be run. Proximity, is an integer value with an order
defined over it. This means, a before property hook with a proximity of 1 will be executed after a before property hook 
withproximity of 2. However, these values are hard coded and there might be unwanted effects when there are other 
hooks and the order of execution is wrong.
