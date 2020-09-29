package org.testcontainers.junit.jqwik;


import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


@Disabled
@Testcontainers
class WrongAnnotationUsageTests {

    @TestContainer
    private String notStartable = "foobar";

    @Test
    void extension_throws_exception() {
        assert true;
    }

}
