package org.testcontainers.client.executionmodes;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.testcontainers.client.commandparts.PumbaCommandPart;

/**
 * Created by novy on 03.06.17.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class PumbaExecutionMode implements PumbaCommandPart {
    private final PumbaCommandPart schedulePart;
    private final PumbaCommandPart containersToAffect;

    @Override
    public String evaluate() {
        return schedulePart.append(containersToAffect).evaluate();
    }
}
