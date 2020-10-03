package org.testcontainers.junit.jqwik;

import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.LifecycleContext;
import net.jqwik.api.lifecycle.SkipExecutionHook.SkipResult;

import java.lang.annotation.Annotation;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestcontainersExtensionTests {

    @Example
    void whenDisabledWithoutDockerAndDockerIsAvailableTestsAreEnabled() {
        SkipResult result = new TestTestcontainersExtension(true)
            .shouldBeSkipped(context(createTestcontainersAnnotation(true)));
        assertThat(result.isSkipped()).isFalse();
    }

    @Example
    void whenDisabledWithoutDockerAndDockerIsUnavailableTestsAreDisabled() {
        SkipResult result = new TestTestcontainersExtension(false)
            .shouldBeSkipped(context(createTestcontainersAnnotation(true)));
        assertThat(result.isSkipped()).isTrue();
    }

    @Example
    void whenEnabledWithoutDockerAndDockerIsAvailableTestsAreEnabled() {
        SkipResult result = new TestTestcontainersExtension(true)
            .shouldBeSkipped(context(createTestcontainersAnnotation(false)));
        assertThat(result.isSkipped()).isFalse();
    }

    @Example
    void whenEnabledWithoutDockerAndDockerIsUnavailableTestsAreEnabled() {
        SkipResult result = new TestTestcontainersExtension(false)
            .shouldBeSkipped(context(createTestcontainersAnnotation(false)));
        assertThat(result.isSkipped()).isFalse();
    }

    private LifecycleContext context(Testcontainers clazz) {
        LifecycleContext extensionContext = mock(LifecycleContext.class);
        when(extensionContext.findAnnotationsInContainer(any())).thenReturn(Collections.singletonList(clazz));
        return extensionContext;
    }

    private Testcontainers createTestcontainersAnnotation(boolean disableWithoutDocker) {
        return new Testcontainers(){
            @Override
            public Class<? extends Annotation> annotationType() {
                return Annotation.class;
            }

            @Override
            public boolean disabledWithoutDocker() {
                return disableWithoutDocker;
            }
        };
    }

    static final class TestTestcontainersExtension extends TestcontainersExtension {

        private final boolean dockerAvailable;

        private TestTestcontainersExtension(boolean dockerAvailable) {
            this.dockerAvailable = dockerAvailable;
        }

        boolean isDockerAvailable() {
            return dockerAvailable;
        }

    }

}
