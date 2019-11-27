package org.testcontainers.containers.rabbitmq.admin;


import org.testcontainers.containers.RabbitMQContainer;

/**
 * Static factory methods for creating {@link DeclareCommand}s
 * to pass to {@link RabbitMQContainer#declare(DeclareCommand)}
 * or {@link DeclareVhostCommand#declare(DeclareCommand)}.
 *
 * <p>Consider static importing the methods in this class for more fluent code.
 * For example:</p>
 *
 * <pre>
 *     import static org.testcontainers.containers.rabbitmq.admin.DeclareCommands.queue;
 *
 *     container.declare(queue("myQueue").autoDelete());
 * </pre>
 */
public class DeclareCommands {

    private DeclareCommands() {
    }

    /**
     * Creates and returns a command to declare a binding.
     * 
     * @param source source of the binding (e.g. the name of an exchange)
     * @param destination destination of the binding (e.g. the name of a queue)
     * @return a command to declare a binding.
     */
    public static DeclareBindingCommand binding(String source, String destination) {
        return new DeclareBindingCommand(source, destination);
    }

    /**
     * Creates and returns a command to declare an exchange.
     *
     * <p>Note the exchange type must be set after construction.
     * For example:</p>
     * 
     * <pre>
     *     exchange("myexchange").direct()
     * </pre>
     * 
     * @param name the name of the exchange
     * @return a command to declare an exchange.
     */
    public static DeclareExchangeCommand exchange(String name) {
        return new DeclareExchangeCommand(name);
    }

    /**
     * Creates and returns a command to declare an operator policy.
     * 
     * <p>The policy definition and pattern must be set after construction.</p>
     * 
     * @param name the name of the policy
     * @return a command to declare an operator policy.
     */
    public static DeclareOperatorPolicyCommand operatorPolicy(String name) {
        return new DeclareOperatorPolicyCommand(name);
    }

    /**
     * Creates and returns a command to declare a parameter.
     * 
     * @param component component to which the parameter applies
     * @param name name of the parameter
     * @param value value of the parameter
     * @return a command to declare a parameter.
     */
    public static DeclareParameterCommand parameter(String component, String name, String value) {
        return new DeclareParameterCommand(component, name, value);
    }

    /**
     * Creates and returns a command to declare a permission.
     *
     * <p>The user must be declared first.</p>
     * 
     * <p>The permission's vhost defaults to the default vhost ({@value DeclareCommand#DEFAULT_VHOST}),
     * and no configure/write/read access.</p>
     * 
     * @param user the user for which to declare the permission.
     * @return a command to declare a permission.
     */
    public static DeclarePermissionCommand permission(String user) {
        return new DeclarePermissionCommand(user);
    }

    /**
     * Creates and returns a command to declare a policy.
     *
     * <p>The policy definition and pattern must be set after construction.</p>
     *
     * @param name the name of the policy
     * @return a command to declare a policy
     */
    public static DeclarePolicyCommand policy(String name) {
        return new DeclarePolicyCommand(name);
    }

    /**
     * Creates and returns a command to declare a queue.
     *
     * @param name name of the queue
     * @return a command to declare a queue.
     */
    public static DeclareQueueCommand queue(String name) {
        return new DeclareQueueCommand(name);
    }

    /**
     * Creates and returns a command to declare a user.
     *
     * @param user the name of the user
     * @return a command to declare a user
     */
    public static DeclareUserCommand user(String user) {
        return new DeclareUserCommand(user);
    }

    /**
     * Creates and returns a command to declare a vhost.
     *
     * <p>The {@link DeclareVhostCommand#declare(DeclareCommand)}
     * method can be used to declare objects within the vhost.</p>
     *
     * @param name the name of the vhost.
     * @return a command to declare a vhost
     */
    public static DeclareVhostCommand vhost(String name) {
        return new DeclareVhostCommand(name);
    }

    /**
     * Creates and returns a command to declare a vhost limit.
     *
     * @param name the name of the limit (see constants in {@link DeclareVhostLimitCommand})
     * @param value the limit value
     * @return a command to declare a vhost limit.
     */
    public static DeclareVhostLimitCommand vhostLimit(String name, int value) {
        return new DeclareVhostLimitCommand(name, value);
    }
}
