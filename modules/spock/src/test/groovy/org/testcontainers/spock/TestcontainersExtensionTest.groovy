package org.testcontainers.spock

import org.spockframework.runtime.model.SpecInfo
import spock.lang.Specification
import spock.lang.Unroll

class TestcontainersExtensionTest extends Specification {

	@Unroll
	def "should handle disabledWithoutDocker=#disabledWithoutDocker and dockerAvailable=#dockerAvailable correctly"() {
		given:
		def dockerDetector = Mock(DockerAvailableDetector)
		dockerDetector.isDockerAvailable() >> dockerAvailable
		def extension = new TestcontainersExtension(dockerDetector)
		def specInfo = Mock(SpecInfo)
		def annotation = disabledWithoutDocker ?
				TestDisabledWithoutDocker.getAnnotation(Testcontainers) :
				TestEnabledWithoutDocker.getAnnotation(Testcontainers)

		when:
		extension.visitSpecAnnotation(annotation, specInfo)

		then:
		skipCalls * specInfo.skip("disabledWithoutDocker is true and Docker is not available")

		where:
		disabledWithoutDocker | dockerAvailable | skipCalls
		true                  | true            | 0
		true                  | false           | 1
		false                 | true            | 0
		false                 | false           | 0
	}

	@Testcontainers(disabledWithoutDocker = true)
	static class TestDisabledWithoutDocker {}

	@Testcontainers
	static class TestEnabledWithoutDocker {}
}
