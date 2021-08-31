package org.testcontainers.controller.model;

import lombok.Getter;

@Getter
public class MountPoint {

    private final String path;
    private final BindMode bindMode;

    public MountPoint(String path, BindMode bindMode){
        this.path = path;
        this.bindMode = bindMode;
    }


    public MountPoint(String path) {
        this(path, BindMode.READ_WRITE);
    }
}
