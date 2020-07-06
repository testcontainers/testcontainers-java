package org.testcontainers.dockerclient;

/**
 * @deprecated no longer needed since `docker-java` supports all transport modes now
 */
@Deprecated
public class ProxiedUnixSocketClientProviderStrategy extends UnixSocketClientProviderStrategy {

    public static final int PRIORITY = EnvironmentAndSystemPropertyClientProviderStrategy.PRIORITY - 100;

    @Override
    protected boolean isApplicable() {
        return false;
    }

    @Override
    protected int getPriority() {
        return PRIORITY;
    }

    @Override
    public void test() throws InvalidConfigurationException {
        throw new InvalidConfigurationException("Deprecated");
    }

    @Override
    public String getDescription() {
        return "[Deprecated] local Unix socket (via TCP proxy)";
    }

}
