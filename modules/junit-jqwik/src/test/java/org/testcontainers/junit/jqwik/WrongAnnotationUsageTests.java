package org.testcontainers.junit.jqwik;


import net.jqwik.api.Disabled;
import net.jqwik.api.Example;

@Disabled
@Testcontainers
class WrongAnnotationUsageTests {

    @Container
    private String notStartable = "foobar";

    @Example
    void extension_throws_exception() {
        assert true;
    }

}
