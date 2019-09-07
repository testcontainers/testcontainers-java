package rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.RabbitMQContainer;

public class RabbitMQAdminUserTest {

    @ClassRule
    public static RabbitMQContainer rabbitmq = new RabbitMQContainer()
        .withAdminPassword("12345");

    @Test
    public void test() {
        assertThat(rabbitmq.isRunning()).isTrue();
        assertThat(rabbitmq.getAdminUsername()).isEqualTo("guest");
        assertThat(rabbitmq.getAdminPassword()).isEqualTo("12345");
    }

}
