package rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
// imports {
import static org.testcontainers.containers.rabbitmq.admin.DeclareCommands.binding;
import static org.testcontainers.containers.rabbitmq.admin.DeclareCommands.exchange;
import static org.testcontainers.containers.rabbitmq.admin.DeclareCommands.permission;
import static org.testcontainers.containers.rabbitmq.admin.DeclareCommands.queue;
import static org.testcontainers.containers.rabbitmq.admin.DeclareCommands.user;
import static org.testcontainers.containers.rabbitmq.admin.DeclareCommands.vhost;
// }

import java.io.IOException;

import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.RabbitMQContainer;

public class RabbitMQDeclareTest {

    // declares {
    @ClassRule
    public static RabbitMQContainer rabbitmq = new RabbitMQContainer()
        .declare(exchange("myExchange").direct())
        .declare(queue("myQueue"))
        .declare(binding("myExchange", "myQueue"))
        .declare(user("skroob").password("12345"))
        .declare(permission("skroob").read(".*"))
        .declare(vhost("myVhost")
            .declare(exchange("myExchangeInMyVhost").direct())
            .declare(queue("myQueueInMyVhost")
                .autoDelete()
                .maxLength(1_000)
                .messageTtl(1_000L))
        );
    // }

    @Test
    public void test() throws IOException, InterruptedException {
        assertTrue(rabbitmq.isRunning());

        assertThat(rabbitmq.execInContainer("rabbitmqadmin", "--vhost=/", "list", "exchanges")
            .getStdout())
            .contains("myExchange")
            .doesNotContain("myExchangeInMyVhost");

        assertThat(rabbitmq.execInContainer("rabbitmqadmin", "--vhost=/", "list", "queues")
            .getStdout())
            .contains("myQueue");

        assertThat(rabbitmq.execInContainer("rabbitmqadmin", "--vhost=/", "list", "bindings")
            .getStdout())
            .contains("myExchange");

        assertThat(rabbitmq.execInContainer("rabbitmqadmin", "--vhost=/", "list", "vhosts")
            .getStdout())
            .contains("myVhost");

        assertThat(rabbitmq.execInContainer("rabbitmqadmin", "--vhost=myVhost", "list", "exchanges")
            .getStdout())
            .contains("myExchangeInMyVhost");

        assertThat(rabbitmq.execInContainer("rabbitmqadmin", "--vhost=myVhost", "list", "queues")
            .getStdout())
            .contains("myQueueInMyVhost");

    }
}
