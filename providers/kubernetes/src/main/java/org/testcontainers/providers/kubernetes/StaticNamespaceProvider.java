package org.testcontainers.providers.kubernetes;

public class StaticNamespaceProvider implements NamespaceProvider {

    private final String namespace;

    public StaticNamespaceProvider(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }
}
