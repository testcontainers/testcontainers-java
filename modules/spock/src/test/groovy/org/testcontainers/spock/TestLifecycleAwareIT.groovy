package org.testcontainers.spock

import org.intellij.lang.annotations.Language
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.EmbeddedSpecRunner

class TestLifecycleAwareIT extends Specification {

    @Unroll("When failing test is #fails, afterTest receives '#filesystemFriendlyNames' and throwable starting with message '#errorMessageStartsWith'")
    def "lifecycle awareness"() {
        given:

        @Language("groovy")
        String myTest = """
import org.testcontainers.spock.Testcontainers
import org.testcontainers.containers.GenericContainer
import spock.lang.Specification

@Testcontainers
class TestLifecycleAwareIT extends Specification {

    GenericContainer container = System.properties["org.testcontainers.container"] as GenericContainer

    def "perform test"() {
        expect:
        !System.properties["org.testcontainers.shouldFail"]
    }
}
"""
        and:
        def container = new TestLifecycleAwareContainerMock()
        System.properties["org.testcontainers.container"] = container
        System.properties["org.testcontainers.shouldFail"] = fails

        when: "executing the test"
        def runner = new EmbeddedSpecRunner(throwFailure: false)
        runner.run(myTest)

        then: "mock container received lifecycle calls as expected"
        container.lifecycleMethodCalls == ["beforeTest", "afterTest"]
        container.lifecycleFilesystemFriendlyNames.join(",") == filesystemFriendlyNames
        if (errorMessageStartsWith) {
            assert container.capturedThrowable.message.startsWith(errorMessageStartsWith)
        } else {
            assert container.capturedThrowable == null
        }

        where:
        fails | filesystemFriendlyNames             | errorMessageStartsWith
        false | 'TestLifecycleAwareIT-perform+test' | null
        true  | 'TestLifecycleAwareIT-perform+test' | "Condition not satisfied:"
    }

}
