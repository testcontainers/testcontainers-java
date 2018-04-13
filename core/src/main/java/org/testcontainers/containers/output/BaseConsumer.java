package org.testcontainers.containers.output;

import lombok.Getter;
import lombok.Setter;

import java.util.function.Consumer;

public abstract class BaseConsumer<S extends BaseConsumer<S>> implements Consumer<OutputFrame> {
    @Getter
    @Setter
    private boolean removeColorCodes = true;

    public S withRemoveAnsiCodes(boolean removeAnsiCodes) {
        this.removeColorCodes = removeAnsiCodes;
        return (S) this;
    }
}
