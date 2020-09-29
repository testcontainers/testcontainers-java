package org.testcontainers.junit.jqwik;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.Tuple.Tuple2;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.testcontainers.junit.jqwik.FilesystemFriendlyNameGenerator.filesystemFriendlyNameOf;

class FilesystemFriendlyNameGeneratorTest {

    @Property
    void should_generate_filesystem_friendly_name(@ForAll("displayNamesAndFilesystemFriendlyNames") Tuple2<String, String> displayNameToExpectedName) {
        String displayName = displayNameToExpectedName.get1();
        String expectedName = displayNameToExpectedName.get2();
        ExtensionContext context = mock(ExtensionContext.class);
        doReturn(displayName)
            .when(context).getUniqueId();

        String filesystemFriendlyName = filesystemFriendlyNameOf(context);

        assertThat(filesystemFriendlyName).isEqualTo(expectedName);
    }

    @Provide
    Arbitrary<Tuple2<String, String>> displayNamesAndFilesystemFriendlyNames() {
        return Arbitraries.of(
            Tuple.of("", "unknown"),
            Tuple.of("  ", "unknown"),
            Tuple.of("not blank", "not+blank"),
            Tuple.of("abc ABC 1234567890", "abc+ABC+1234567890"),
            Tuple.of(
                "no_umlauts_äöüÄÖÜéáíó",
                "no_umlauts_%C3%A4%C3%B6%C3%BC%C3%84%C3%96%C3%9C%C3%A9%C3%A1%C3%AD%C3%B3"
            ),
            Tuple.of(
                "[engine:junit-jupiter]/[class:com.example.MyTest]/[test-factory:parameterizedTest()]/[dynamic-test:#3]",
                "%5Bengine%3Ajunit-jupiter%5D%2F%5Bclass%3Acom.example.MyTest%5D%2F%5Btest-factory%3AparameterizedTest%28%29%5D%2F%5Bdynamic-test%3A%233%5D"
            )
        );
    }
}
