package org.testcontainers.controller.model;

import com.github.dockerjava.api.model.SELContext;
import lombok.Getter;

@Getter
public class HostMount {
    private final String hostPath;
    private final MountPoint mountPoint;
    private final SELContext selContext; // TODO: Replace SELContext

    public HostMount(String hostPath, MountPoint mountPoint, SELContext selContext) {
        this.hostPath = hostPath;
        this.mountPoint = mountPoint;
        this.selContext = selContext;
    }

    public HostMount(String hostPath, MountPoint mountPoint) {
        this(hostPath, mountPoint, SELContext.none);
    }
}
