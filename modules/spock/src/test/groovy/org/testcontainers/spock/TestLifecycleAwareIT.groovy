package org.testcontainers.spock

import org.intellij.lang.annotations.Language
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.EmbeddedSpecRunner

class TestLifecycleAwareIT extends Specification {

    @Unroll("When failing test is #fails afterTest receives '#filesystemFriendlyNames'")
    def "lifecycle awareness"() {
        given:

        //noinspection GrPackage
        @Language("groovy")
        String myTest = """
package org.testcontainers.spock

import org.testcontainers.containers.GenericContainer
import spock.lang.Specification

@Testcontainers
class TestLifecycleAwareIT extends Specification {

    GenericContainer container = System.properties["org.testcontainers.container"]

    def "perform test"() {
        expect:
        "false" == System.getProperty("org.testcontainers.shouldFail")
    }
}
"""

        when:
        def container = new TestLifecycleAwareContainerMock()
        def runner = new EmbeddedSpecRunner(throwFailure: false)
        System.properties["org.testcontainers.container"] = container
        System.setProperty("org.testcontainers.shouldFail", fails.toString())
        runner.run(myTest)

        then:
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
