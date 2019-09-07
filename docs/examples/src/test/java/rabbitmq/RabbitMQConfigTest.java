package rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.MountableFile;

public class RabbitMQConfigTest {

    // config {
    @ClassRule
    public static RabbitMQContainer rabbitmq = new RabbitMQContainer()
        .withRabbitMQConfig(MountableFile.forClasspathResource("/rabbitmq/rabbitmq-custom.conf"));
    // }

    @Test
    public void test() {
        assertThat(rabbitmq.isRunning()).isTrue();
        assertThat(rabbitmq.getLogs()).contains("config file(s) : /etc/rabbitmq/rabbitmq-custom.conf");
    }

}
