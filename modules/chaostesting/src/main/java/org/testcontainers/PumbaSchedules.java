package org.testcontainers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Created by novy on 01.01.17.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PumbaSchedules {

    public static PumbaSchedule onlyOnce() {
        return () -> "";
    }

    public interface PumbaSchedule extends PumbaCommandPart {
    }
}
