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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotatedMethods;

@ExtendWith({
    TestcontainersSharedContainerWithGeneratedResourcesTests.RunCodeExtension.class,
    TestcontainersExtension.class,
})
class TestcontainersSharedContainerWithGeneratedResourcesTests {
    @TempDir
    static File tempDir;

    @Container
    private static GenericContainer<?> NGINX = new GenericContainer<>("nginx:1.9.4")
        .withFileSystemBind(() -> dirFe(tempDir, "fe").getAbsolutePath(), "/usr/share/nginx/html", BindMode.READ_ONLY)
        .withExposedPorts(80)
        .waitingFor(new HttpWaitStrategy());

    private static String lastContainerId;

    @RunCodeExtension.RunBeforeCustomExtensions
    private static void runBeforeAll(ExtensionContext context) throws IOException {
        final File dirFe = dirFe(tempDir, "fe");
        dirFe.mkdirs();

        try (final OutputStream out = new BufferedOutputStream(new FileOutputStream(new File(dirFe, "index.html")))) {
            IOUtils.write("<html><body>Hello from FE!</body></html>", out, StandardCharsets.UTF_8);
        }
    }

    @BeforeAll
    public static void setupAll() {
        assertTrue(NGINX.isRunning(), "");
    }

    @Test
    void httpGetOfCorrectBindOfGeneratedFile() throws IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(new HttpGet("http://" + NGINX.getContainerIpAddress() + ":" + NGINX.getMappedPort(80)));

        assertEquals(200, response.getStatusLine().getStatusCode());
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        response.getEntity().writeTo(buffer);
        final String body = buffer.toString(StandardCharsets.UTF_8.displayName());
        assertEquals("<html><body>Hello from FE!</body></html>", body);
    }

    @Test
    void firstTestThatOneContainerCreated() {
        if (lastContainerId == null) {
            lastContainerId = NGINX.getContainerId();
        } else {
            assertEquals(lastContainerId, NGINX.getContainerId());
        }
    }

    @Test
    void secondTestThatOneContainerCreated() {
        if (lastContainerId == null) {
            lastContainerId = NGINX.getContainerId();
        } else {
            assertEquals(lastContainerId, NGINX.getContainerId());
        }
    }

    private static File dirFe(File parent, String dir) {
        return new File(parent, dir);
    }

    public static class RunCodeExtension implements BeforeAllCallback {
        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
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
