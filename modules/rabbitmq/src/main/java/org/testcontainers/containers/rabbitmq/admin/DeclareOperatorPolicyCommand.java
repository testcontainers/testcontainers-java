package org.testcontainers.containers.rabbitmq.admin;

/**
 * Declares an operator policy.
 *
 * <p>Note that the policy definition and pattern must be set after construction.</p>
 */
public class DeclareOperatorPolicyCommand extends DeclarePolicyBaseCommand<DeclareOperatorPolicyCommand> {

    public DeclareOperatorPolicyCommand(String name) {
        super("operator_policy", name);
    }
}
