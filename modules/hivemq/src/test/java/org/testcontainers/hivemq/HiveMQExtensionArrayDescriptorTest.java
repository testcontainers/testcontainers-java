package org.testcontainers.hivemq;

import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class HiveMQExtensionArrayDescriptorTest {

    @Test
    void createExtension_ignoresArrayDescriptorsInConstantPool() throws Exception {
        final HiveMQExtension extension = HiveMQExtension
            .builder()
            .id("my-extension")
            .name("My Extension")
            .version("1.0.0")
            .mainClass(NoopExtension.class)
            .addAdditionalClass(ArrayHolder.class)
            .build();

        final File extensionDir = extension.createExtension(extension);

        assertThat(new File(extensionDir, "extension.jar")).exists();
    }

    public static class NoopExtension implements ExtensionMain {

        @Override
        public void extensionStart(
            final ExtensionStartInput extensionStartInput,
            final ExtensionStartOutput extensionStartOutput
        ) {}

        @Override
        public void extensionStop(
            final ExtensionStopInput extensionStopInput,
            final ExtensionStopOutput extensionStopOutput
        ) {}
    }

    /**
     * Referencing array classes forces their descriptors ([B, [J, [[Ljava/lang/String;) into the
     * constant pool of this class, which previously made {@link HiveMQExtension#createExtension} fail
     * with a {@link java.util.regex.PatternSyntaxException}.
     */
    public static class ArrayHolder {

        @SuppressWarnings("unused")
        private static final Class<?> BYTE_ARRAY_CLASS = byte[].class;

        @SuppressWarnings("unused")
        private static final Class<?> LONG_ARRAY_CLASS = long[].class;

        @SuppressWarnings("unused")
        private static final Class<?> MULTIDIMENSIONAL_OBJECT_ARRAY_CLASS = String[][].class;
    }
}
