package org.testcontainers.containers;

import com.github.dockerjava.api.model.SELContext;
import lombok.AllArgsConstructor;

/**
 * Possible contexts for use with SELinux
 */
@AllArgsConstructor
public enum SelinuxContext {
    SHARED(SELContext.shared),
    SINGLE(SELContext.single),
    NONE(SELContext.none);

    public final SELContext selContext;

}
