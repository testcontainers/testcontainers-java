package org.testcontainers.lifecycle;

import java.util.Optional;

public interface TestLifecycleAware {

    default void beforeTestBlock(TestDescription description) {

    }

    default void afterTestBlock(TestDescription description, Optional<Throwable> throwable) {

    }
}
