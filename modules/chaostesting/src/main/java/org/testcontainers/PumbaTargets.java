package org.testcontainers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Created by novy on 01.01.17.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PumbaTargets {

    public static PumbaTarget singleContainer(String containerName) {
        return () -> containerName;
    }

    public static PumbaTarget allContainers() {
        return () -> "";
    }

    public interface PumbaTarget extends PumbaCommandPart {
    }
}
