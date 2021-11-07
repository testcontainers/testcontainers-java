package org.testcontainers.junit.jupiter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.commons.util.ReflectionUtils;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.LazyFileSystemBind;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotatedMethods;

@ExtendWith({
    PublicTestcontainersExtensionTests.RunCodeExtension.class,
    TestcontainersExtension.class
})
class PublicTestcontainersExtensionTests {
    @TempDir
    static File tempDir;

    @Container
    private static final GenericContainer<?> nginx = new GenericContainer<>("nginx:1.9.4")
        .withFileSystemBind(new LazyFileSystemBind.Builder()
            .withHostPath(() -> myDir(tempDir, "nginx").getAbsolutePath())
            .withContainerPath("/usr/share/nginx/html")
            .withMode(BindMode.READ_ONLY)
            .build())
        .withExposedPorts(80)
        .waitingFor(new HttpWaitStrategy());

    @RunCodeExtension.RunBeforeCustomExtensions
    private static void generateHtmlFiles(ExtensionContext context) throws IOException {
        final File dirFe = myDir(tempDir, "nginx");
        dirFe.mkdirs();

        try (final OutputStream out = new BufferedOutputStream(new FileOutputStream(new File(dirFe, "index.html")))) {
            IOUtils.write("<html><body>Hello from nginx!</body></html>", out, StandardCharsets.UTF_8);
        }
    }

    @BeforeAll
    public static void beforeAll() {
        assertThat(nginx.isRunning())
            .describedAs("nginx should be running")
            .isTrue();
    }

    @Test
    void httpGetFromNginxStatic() throws IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(new HttpGet("http://" + nginx.getContainerIpAddress() + ":" + nginx.getMappedPort(80)));

        assertThat(response.getStatusLine().getStatusCode())
            .isEqualTo(200);
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        response.getEntity().writeTo(buffer);
        final String body = buffer.toString(StandardCharsets.UTF_8.displayName());
        assertThat(body)
            .isEqualTo("<html><body>Hello from nginx!</body></html>");
    }

    private static File myDir(File parent, String dir) {
        return new File(Objects.requireNonNull(parent, "parent dir cannot be null"), dir);
    }

    public static class RunCodeExtension implements BeforeAllCallback {
        @Override
        public void beforeAll(ExtensionContext context) {
            context.getTestInstance();
            findAnnotatedMethods(context.getRequiredTestClass(), RunBeforeCustomExtensions.class, ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .stream()
                .filter(ReflectionUtils::isStatic)
                .filter(it -> it.getParameterCount() == 1)
                .filter(it -> ClassUtils.isAssignable(it.getParameterTypes()[0], ExtensionContext.class))
                .map(ReflectionUtils::makeAccessible)
                .forEach(method -> {
                    try {
                        method.invoke(null, context);
                    } catch (Throwable t) {
                        ExceptionUtils.throwAsUncheckedException(t);
                    }
                });
        }

        @Target({ElementType.METHOD})
        @Retention(RetentionPolicy.RUNTIME)
        @Documented
        public @interface RunBeforeCustomExtensions {
        }
    }
}
