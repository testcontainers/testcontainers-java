package org.testcontainers.junit.jupiter;


import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


@Disabled
@Testcontainers
class WrongAnnotationUsageTests {

    @Container
    private String notStartable = "foobar";

    @Test
    void extension_throws_exception() {
        assert true;
    }

}
