package org.testcontainers.containers.output;

import lombok.Getter;
import lombok.Setter;

import java.util.function.Consumer;

public abstract class BaseConsumer<SELF extends BaseConsumer<SELF>> implements Consumer<OutputFrame> {
    @Getter
    @Setter
    private boolean removeColorCodes = true;

    public SELF withRemoveAnsiCodes(boolean removeAnsiCodes) {
        this.removeColorCodes = removeAnsiCodes;
        return (SELF) this;
    }
}
