package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.testcontainers.junit.jupiter.FilesystemFriendlyNameGenerator.filesystemFriendlyNameOf;

class FilesystemFriendlyNameGeneratorTest {

    @ParameterizedTest
    @MethodSource("provideDisplayNamesAndFilesystemFriendlyNames")
    void should_generate_filesystem_friendly_name(String displayName, String expectedName) {
        ExtensionContext context = mock(ExtensionContext.class);
        doReturn(displayName)
            .when(context).getDisplayName();

        String filesystemFriendlyName = filesystemFriendlyNameOf(context);

        assertThat(filesystemFriendlyName).isEqualTo(expectedName);
    }

    private static Stream<Arguments> provideDisplayNamesAndFilesystemFriendlyNames() {
        return Stream.of(
            Arguments.of("", "unknown"),
            Arguments.of("  ", "unknown"),
            Arguments.of("not blank", "not blank"),
            Arguments.of("abc ABC 1234567890", "abc ABC 1234567890"),
            Arguments.of("no_umlauts_äöüÄÖÜéáíó", "no_umlauts_"),
            Arguments.of("no\ttabs", "notabs"),
            Arguments.of("no_special_[]{}/?<>!@#$%^&*()+=\\|'\";:`~", "no_special_")
        );
    }
}
