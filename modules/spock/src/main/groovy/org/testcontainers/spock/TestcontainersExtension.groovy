package org.testcontainers.spock

import groovy.transform.CompileStatic
import org.spockframework.runtime.AbstractRunListener
import org.spockframework.runtime.extension.AbstractAnnotationDrivenExtension
import org.spockframework.runtime.model.ErrorInfo
import org.spockframework.runtime.model.SpecInfo

@CompileStatic
class TestcontainersExtension extends AbstractAnnotationDrivenExtension<Testcontainers> {

	private final DockerAvailableDetector dockerDetector

	TestcontainersExtension() {
		this(new DockerAvailableDetector())
	}

	TestcontainersExtension(DockerAvailableDetector dockerDetector) {
		this.dockerDetector = dockerDetector
	}

	@Override
	void visitSpecAnnotation(Testcontainers annotation, SpecInfo spec) {
		if (annotation.disabledWithoutDocker()) {
			if (!dockerDetector.isDockerAvailable()) {
				spec.skip("disabledWithoutDocker is true and Docker is not available")
			}
		}
		def listener = new ErrorListener()
		def interceptor = new TestcontainersMethodInterceptor(spec, listener)
		spec.addSetupSpecInterceptor(interceptor)
		spec.addCleanupSpecInterceptor(interceptor)
		spec.addSetupInterceptor(interceptor)
		spec.addCleanupInterceptor(interceptor)

		spec.addListener(listener)
	}

	class ErrorListener extends AbstractRunListener {
		List<ErrorInfo> errors = []

		@Override
		void error(ErrorInfo error) {
			errors.add(error)
		}
	}
}
