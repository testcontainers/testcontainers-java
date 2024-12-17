package org.testcontainers.containers;

import java.util.Set;

class ComposeCommand {

    static String getDownCommand(ComposeDelegate.ComposeVersion composeVersion, Set<String> options) {
        String composeOptions = optionsAsString(options);
        if (composeOptions.isEmpty()) {
            return composeVersion == ComposeDelegate.ComposeVersion.V1 ? "down" : "compose down";
        }
        String cmd = composeVersion == ComposeDelegate.ComposeVersion.V1 ? "%s down" : "compose %s down";
        return String.format(cmd, composeOptions);
    }

    static String getUpCommand(ComposeDelegate.ComposeVersion composeVersion, Set<String> options) {
        String composeOptions = optionsAsString(options);
        if (composeOptions.isEmpty()) {
            return composeVersion == ComposeDelegate.ComposeVersion.V1 ? "up -d" : "compose up -d";
        }
        String cmd = composeVersion == ComposeDelegate.ComposeVersion.V1 ? "%s up -d" : "compose %s up -d";
        return String.format(cmd, composeOptions);
    }

    private static String optionsAsString(final Set<String> options) {
        String optionsString = String.join(" ", options);
        if (!optionsString.isEmpty()) {
            // ensures that there is a space between the options and 'up' if options are passed.
            return optionsString;
        } else {
            // otherwise two spaces would appear between 'docker-compose' and 'up'
            return "";
        }
    }
}
