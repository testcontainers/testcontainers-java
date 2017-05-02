package org.testcontainers.containers;

import com.github.dockerjava.api.model.SELContext;

/**
 * Possible contexts for use with SELinux
 */
public enum SelinuxContext {
    SHARED(SELContext.shared), SINGLE(SELContext.single), NONE(SELContext.none);

    public final SELContext selContext;

    SelinuxContext(final SELContext selContext) {
        this.selContext = selContext;
    }
}
