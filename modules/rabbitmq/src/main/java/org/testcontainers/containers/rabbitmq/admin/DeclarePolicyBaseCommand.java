package org.testcontainers.containers.rabbitmq.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

/**
 * Base class for declaring policy and operator policy.
 *
 * <p>Note that the policy definition and pattern must be set after construction.</p>
 *
 * @param <SELF> the type of command (used for the return type of fluent setter methods)
 */
public abstract class DeclarePolicyBaseCommand<SELF extends DeclarePolicyBaseCommand> extends DeclareCommand<SELF> {

    private final String name;

    private String pattern;

    private Integer priority;

    private String applyTo;

    private final Map<String, Object> definition = new HashMap<>();

    protected DeclarePolicyBaseCommand(String objectType, String name) {
        super(objectType);
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    public SELF pattern(String pattern) {
        this.pattern = Objects.requireNonNull(pattern, "pattern must not be null");
        return self();
    }

    public SELF priority(Integer priority) {
        this.priority = priority;
        return self();
    }

    public SELF applyTo(String applyTo) {
        this.applyTo = applyTo;
        return self();
    }

    /**
     * Appends the given name/value to the policy definition.
     *
     * @param name the definition name
     * @param value the definition value
     * @return this command.
     */
    public SELF definition(String name, Object value) {
        this.definition.put(name, value);
        verifyCanConvertToJson(this.definition);
        return self();
    }
    /**
     * Appends the given name/value pairs to the policy definition.
     *
     * @param definition the policy definition
     * @return this command.
     */
    public SELF definition(Map<String, Object> definition) {
        this.definition.putAll(definition);
        verifyCanConvertToJson(this.definition);
        return self();
    }

    @Override
    @NotNull
    protected List<String> getCliArguments() {

        if (definition.isEmpty()) {
            throw new IllegalArgumentException("policy definition must be specified for " + getObjectType() + " with name " + name);
        }

        List<String> cliArguments = new ArrayList<>();
        cliArguments.add(cliArgument("name", name));
        cliArguments.add(cliArgument("pattern", Objects.requireNonNull(pattern, () -> "pattern must be specified for " + getObjectType() + " with name " + name)));
        cliArguments.add(cliArgument("definition", definition));

        if (priority != null) {
            cliArguments.add(cliArgument("priority", priority.toString()));
        }
        if (applyTo != null) {
            cliArguments.add(cliArgument("apply-to", applyTo));
        }

        return cliArguments;
    }
}
