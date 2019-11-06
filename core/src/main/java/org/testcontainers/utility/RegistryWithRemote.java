package org.testcontainers.utility;

import lombok.Data;

@Data
class RegistryWithRemote {
    private final String registry;
    private final String remoteName;

    static RegistryWithRemote from(String name) {
        final int slashIndex = name.indexOf('/');

        if (slashIndex == -1 ||
            (!name.substring(0, slashIndex).contains(".") &&
                !name.substring(0, slashIndex).contains(":") &&
                !name.substring(0, slashIndex).equals("localhost"))) {
            return new RegistryWithRemote("", name);
        } else {
            return new RegistryWithRemote(name.substring(0, slashIndex), name.substring(slashIndex + 1));
        }
    }
}
