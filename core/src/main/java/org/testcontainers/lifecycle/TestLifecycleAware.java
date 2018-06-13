package org.testcontainers.lifecycle;

import java.util.Optional;

public interface TestLifecycleAware {

    default void beforeTest(TestDescription description) {

    }

    default void afterTest(TestDescription description, Optional<Throwable> throwable) {

    }
}
