package org.testcontainers.client.actions.containeractions;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.testcontainers.client.commandparts.PumbaCommandPart;

/**
 * Created by novy on 17.01.17.
 */

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class RemoveContainers implements ContainerAction {
    private boolean force = false;
    private boolean links = false;
    private boolean volumes = false;

    public RemoveContainers forced() {
        this.force = true;
        return this;
    }

    public RemoveContainers withAssociatedLinks() {
        this.links = true;
        return this;
    }

    public RemoveContainers withAssociatedVolumes() {
        this.volumes = true;
        return this;
    }

    @Override
    public String evaluate() {
        return removeContainersPart()
                .append(forcePart())
                .append(linksPart())
                .append(volumesPart())
                .evaluate();
    }

    private PumbaCommandPart removeContainersPart() {
        return () -> "rm";
    }

    private PumbaCommandPart forcePart() {
        return () -> force ? "--force" : "";
    }

    private PumbaCommandPart linksPart() {
        return () -> links ? "--links" : "";
    }

    private PumbaCommandPart volumesPart() {
        return () -> volumes ? "--volumes" : "";
    }
}
