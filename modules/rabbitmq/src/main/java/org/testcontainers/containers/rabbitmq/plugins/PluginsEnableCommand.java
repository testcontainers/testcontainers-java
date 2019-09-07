package org.testcontainers.containers.rabbitmq.plugins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * A command to enable rabbitmq plugins
 */
public class PluginsEnableCommand extends RabbitMQPluginsCommand {

    private final List<String> plugins = new ArrayList<>();

    public PluginsEnableCommand(String... plugins) {
        super("enable");
        plugins(plugins);
    }

    /**
     * Set the plugins to enable
     * @param plugins the plugins to enable
     * @return this command
     */
    public PluginsEnableCommand plugins(String... plugins) {
        if (plugins != null) {
            this.plugins.addAll(Arrays.asList(plugins));
        }
        return this;
    }

    @Override
    protected @NotNull List<String> getCliArguments() {
        return this.plugins;
    }
}
