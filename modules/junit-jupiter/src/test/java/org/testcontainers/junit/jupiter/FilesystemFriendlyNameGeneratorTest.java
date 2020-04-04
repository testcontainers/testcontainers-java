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
            .when(context).getUniqueId();

        String filesystemFriendlyName = filesystemFriendlyNameOf(context);

        assertThat(filesystemFriendlyName).isEqualTo(expectedName);
    }

    private static Stream<Arguments> provideDisplayNamesAndFilesystemFriendlyNames() {
        return Stream.of(
            Arguments.of("", "unknown"),
            Arguments.of("  ", "unknown"),
            Arguments.of("not blank", "not+blank"),
            Arguments.of("abc ABC 1234567890", "abc+ABC+1234567890"),
            Arguments.of(
                "no_umlauts_äöüÄÖÜéáíó",
                "no_umlauts_%C3%A4%C3%B6%C3%BC%C3%84%C3%96%C3%9C%C3%A9%C3%A1%C3%AD%C3%B3"
            ),
            Arguments.of(
                "[engine:junit-jupiter]/[class:com.example.MyTest]/[test-factory:parameterizedTest()]/[dynamic-test:#3]",
                "%5Bengine%3Ajunit-jupiter%5D%2F%5Bclass%3Acom.example.MyTest%5D%2F%5Btest-factory%3AparameterizedTest%28%29%5D%2F%5Bdynamic-test%3A%233%5D"
            )
        );
    }
}
