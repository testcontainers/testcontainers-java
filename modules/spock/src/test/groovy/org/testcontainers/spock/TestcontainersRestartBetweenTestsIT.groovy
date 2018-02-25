package org.testcontainers.spock

import org.testcontainers.containers.GenericContainer
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

@Stepwise
@Testcontainers
class TestcontainersRestartBetweenTestsIT extends Specification {

    GenericContainer genericContainer = new GenericContainer("httpd:2.4-alpine")
            .withExposedPorts(80)

    @Shared
    String lastContainerId

    def "retrieving first id"() {
        when:
        lastContainerId = genericContainer.containerId

        then:
        true
    }

    def "containers is recreated between tests"() {
        expect:
        genericContainer.containerId != lastContainerId
    }

}
