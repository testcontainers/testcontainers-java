package org.testcontainers;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * This test checks that we don't expose any shaded class in our public API.
 */
@ParameterizedClass
@MethodSource("data")
@RequiredArgsConstructor
public class PublicBinaryAPITest extends AbstractJarFileTest {

    private static String SHADED_PACKAGE = "org.testcontainers.shaded.";

    private static String SHADED_PACKAGE_PATH = SHADED_PACKAGE.replaceAll("\\.", "/");

    static {
        Assertions.registerFormatterForType(ClassNode.class, it -> it.name);
        Assertions.registerFormatterForType(FieldNode.class, it -> it.name);
        Assertions.registerFormatterForType(MethodNode.class, it -> it.name + it.desc);
    }

    public static List<Object[]> data() throws Exception {
        List<Object[]> result = new ArrayList<>();

        Files.walkFileTree(
            root,
            new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    String fileName = path.toString();

                    if (!fileName.endsWith(".class")) {
                        return super.visitFile(path, attrs);
                    }

                    if (!fileName.startsWith("/org/testcontainers/")) {
                        return super.visitFile(path, attrs);
                    }

                    if (fileName.startsWith("/" + SHADED_PACKAGE_PATH)) {
                        return super.visitFile(path, attrs);
                    }

                    try (InputStream inputStream = Files.newInputStream(path)) {
                        ClassReader reader = new ClassReader(inputStream);
                        ClassNode node = new ClassNode();
                        reader.accept(node, ClassReader.SKIP_CODE);
                        if ((node.access & Opcodes.ACC_PUBLIC) != 0) {
                            result.add(new Object[] { fileName, node });
                        }
                    }

                    return super.visitFile(path, attrs);
                }
            }
        );
        return result;
    }

    private final String fileName;

    private final ClassNode classNode;

    @BeforeEach
    public void setUp() {
        switch (classNode.name) {
            // Necessary evil
            case "org/testcontainers/dockerclient/UnixSocketClientProviderStrategy":
            case "org/testcontainers/dockerclient/DockerClientProviderStrategy":
            case "org/testcontainers/dockerclient/WindowsClientProviderStrategy":
            case "org/testcontainers/utility/DynamicPollInterval":
                assumeThat(true).isFalse();
        }
    }

    @Test
    public void testSuperClass() {
        assertThat(classNode.superName).doesNotStartWith(SHADED_PACKAGE_PATH);
    }

    @Test
    public void testInterfaces() {
        assertThat(classNode.interfaces).allSatisfy(it -> assertThat(it).doesNotStartWith(SHADED_PACKAGE_PATH));
    }

    @Test
    public void testMethodReturnTypes() {
        assertThat(classNode.methods)
            .filteredOn(it -> (it.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0)
            .allSatisfy(it -> assertThat(Type.getReturnType(it.desc).getClassName()).doesNotStartWith(SHADED_PACKAGE));
    }

    @Test
    public void testMethodArguments() {
        assertThat(classNode.methods)
            .filteredOn(it -> (it.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0)
            .allSatisfy(method -> {
                assertThat(Arrays.asList(Type.getArgumentTypes(method.desc)))
                    .extracting(Type::getClassName)
                    .allSatisfy(it -> assertThat(it).doesNotStartWith(SHADED_PACKAGE));
            });
    }

    @Test
    public void testFields() {
        assertThat(classNode.fields)
            .filteredOn(it -> (it.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0)
            .allSatisfy(it -> assertThat(Type.getType(it.desc).getClassName()).doesNotStartWith(SHADED_PACKAGE));
    }
}
