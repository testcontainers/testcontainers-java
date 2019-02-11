package org.testcontainers.dockerclient.check;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.testcontainers.utility.TestcontainersConfiguration;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DependencyCheckerTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testIfVersionCheckIsDisabled() {
        TestcontainersConfiguration testcontainersConfiguration = mock(TestcontainersConfiguration.class);
        when(testcontainersConfiguration.isDisableDockerVersionChecks()).thenReturn(true);
        DependencyChecker dependencyChecker = new DependencyChecker(testcontainersConfiguration);
        dependencyChecker.checkDockerVersion("a.b.c");
    }

    @Test
    public void testIfVersionCheckIsEnabled() {
        TestcontainersConfiguration testcontainersConfiguration = mock(TestcontainersConfiguration.class);
        when(testcontainersConfiguration.isDisableDockerVersionChecks()).thenReturn(false);
        DependencyChecker dependencyChecker = new DependencyChecker(testcontainersConfiguration);

        expectedException.expect(NumberFormatException.class);
        expectedException.expectMessage(new BaseMatcher<String>() {
            @Override
            public boolean matches(Object item) {
                if (!(item instanceof String)) {
                    fail("Exception message is not String. Oops.");
                }
                return "For input string: \"a\"".equals(item);
            }

            @Override
            public void describeTo(Description description) {
//               NO-OP
            }
        });

        dependencyChecker.checkDockerVersion("a.b.c");
    }
}
