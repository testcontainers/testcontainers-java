package org.testcontainers.containers.rabbitmq.admin;

/**
 * Declares a policy.
 *
 * <p>Note that the policy definition and pattern must be set after construction.</p>
 */
public class DeclarePolicyCommand extends DeclarePolicyBaseCommand<DeclarePolicyCommand> {

    public DeclarePolicyCommand(String name) {
        super("policy", name);
    }
}
