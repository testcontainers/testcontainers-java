package org.testcontainers.containers.output;

import lombok.Getter;
import lombok.Setter;
import org.testcontainers.containers.output.OutputFrame.OutputType;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.Consumer;

/**
 * Base class for OutputFrame consumers, OutputTypes can be configured (all by default).
 */
public abstract class BaseConsumer<SELF extends BaseConsumer<SELF>> implements Consumer<OutputFrame> {
    @Getter
    @Setter
    private boolean removeColorCodes = true;
    protected OutputType[] types;

    public BaseConsumer() {
        this(OutputType.values());
    }

    public BaseConsumer(OutputType... types) {
        setTypes(types);
    }

    /**
     * @return OutputTypes which will be consumed
     */
    public OutputType[] getTypes() {
        return types;
    }

    /**
     * @param types configure which OutputTypes will be consumed
     */
    @SuppressWarnings("unchecked")
    public SELF setTypes(OutputType... types) {
        this.types = types.length == 0 ? OutputType.values() : EnumSet
            .of(types[0], Arrays.copyOfRange(types, 1, types.length)).toArray(new OutputType[0]);
        return (SELF) this;
    }

    @SuppressWarnings("unchecked")
    public SELF withRemoveAnsiCodes(boolean removeAnsiCodes) {
        this.removeColorCodes = removeAnsiCodes;
        return (SELF) this;
    }

}
