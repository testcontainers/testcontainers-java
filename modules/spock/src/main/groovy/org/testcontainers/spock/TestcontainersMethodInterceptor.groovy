package org.testcontainers.spock

import org.spockframework.runtime.extension.AbstractMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation
import org.spockframework.runtime.model.FieldInfo
import org.spockframework.runtime.model.SpecInfo
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.GenericContainer

class TestcontainersMethodInterceptor extends AbstractMethodInterceptor {

    private final SpecInfo spec

    TestcontainersMethodInterceptor(SpecInfo spec) {
        this.spec = spec
    }

    @Override
    void interceptSetupSpecMethod(IMethodInvocation invocation) throws Throwable {
        def containers = findAllContainers(true)
        startContainers(containers, invocation)

        def compose = findAllComposeContainers(true)
        startComposeContainers(compose, invocation)

        invocation.proceed()
    }

    void interceptCleanupSpecMethod(IMethodInvocation invocation) throws Throwable {
        def containers = findAllContainers(true)
        stopContainers(containers, invocation)

        def compose = findAllComposeContainers(true)
        stopComposeContainers(compose, invocation)

        invocation.proceed()
    }

    @Override
    void interceptSetupMethod(IMethodInvocation invocation) throws Throwable {
        def containers = findAllContainers(false)
        startContainers(containers, invocation)

        def compose = findAllComposeContainers(false)
        startComposeContainers(compose, invocation)

        invocation.proceed()
    }


    @Override
    void interceptCleanupMethod(IMethodInvocation invocation) throws Throwable {
        def containers = findAllContainers(false)
        stopContainers(containers, invocation)

        def compose = findAllComposeContainers(false)
        stopComposeContainers(compose, invocation)

        invocation.proceed()
    }

    private List<FieldInfo> findAllContainers(boolean shared) {
        spec.allFields.findAll { FieldInfo f ->
            GenericContainer.isAssignableFrom(f.type) && f.shared == shared
        }
    }

    private List<FieldInfo> findAllComposeContainers(boolean shared) {
        spec.allFields.findAll { FieldInfo f ->
            DockerComposeContainer.isAssignableFrom(f.type) && f.shared == shared
        }
    }

    private static void startContainers(List<FieldInfo> containers, IMethodInvocation invocation) {
        containers.each { FieldInfo f ->
            GenericContainer container = readContainerFromField(f, invocation)
            if(!container.isRunning()){
                container.start()
            }
        }
    }

    private static void stopContainers(List<FieldInfo> containers, IMethodInvocation invocation) {
        containers.each { FieldInfo f ->
            GenericContainer container = readContainerFromField(f, invocation)
            container.stop()
        }
    }

    private static void startComposeContainers(List<FieldInfo> compose, IMethodInvocation invocation) {
        compose.each { FieldInfo f ->
            DockerComposeContainer c = f.readValue(invocation.instance) as DockerComposeContainer
            c.starting(null)
        }
    }

    private static void stopComposeContainers(List<FieldInfo> compose, IMethodInvocation invocation) {
        compose.each { FieldInfo f ->
            DockerComposeContainer c = f.readValue(invocation.instance) as DockerComposeContainer
            c.finished(null)
        }
    }


    private static GenericContainer readContainerFromField(FieldInfo f, IMethodInvocation invocation) {
        f.readValue(invocation.instance) as GenericContainer
    }
}
