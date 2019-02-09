package org.testcontainers.spock

import org.spockframework.runtime.extension.AbstractAnnotationDrivenExtension
import org.spockframework.runtime.model.SpecInfo

class TestcontainersExtension extends AbstractAnnotationDrivenExtension<Testcontainers> {

    @Override
    void visitSpecAnnotation(Testcontainers annotation, SpecInfo spec) {
        def interceptor = new TestcontainersMethodInterceptor(spec)
        spec.addSetupSpecInterceptor(interceptor)
        spec.addCleanupSpecInterceptor(interceptor)
        spec.addSetupInterceptor(interceptor)
        spec.addCleanupInterceptor(interceptor)
    }

}
