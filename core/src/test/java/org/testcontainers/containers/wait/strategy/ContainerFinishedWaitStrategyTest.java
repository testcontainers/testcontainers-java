package org.testcontainers.containers.wait.strategy;

import org.bouncycastle.asn1.x9.DHDomainParameters;
import org.junit.Test;
import org.testcontainers.TestImages;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ContainerFinishedWaitStrategyTest {

    @Test
    public void shouldWaitUntilContainerClosed() {
        try ( GenericContainer container = new GenericContainer<>(TestImages.TINY_IMAGE)
                .withCommand( "/bin/echo \"hello world\"")
                .withStartupCheckStrategy( new OneShotStartupCheckStrategy() )
                .waitingFor( new ContainerFinishedWaitStrategy() )) {
            container.start();
            assertThat( container.isRunning() ).as( "Wait strategy should wait until container has stopped" ).isFalse();
        }
    }

    @Test
    public void shouldWaitUntilContainerClosed_slow() {
        try ( GenericContainer container = new GenericContainer<>(TestImages.TINY_IMAGE)
                .withCommand( "/bin/sleep", "3s")
                .withStartupCheckStrategy( new OneShotStartupCheckStrategy() )
                .waitingFor( new ContainerFinishedWaitStrategy().withStartupTimeout( Duration.ofSeconds( 5 ))) ) {
            container.start();
            assertThat( container.isRunning() ).as( "Wait strategy should wait until container has stopped" ).isFalse();
        }
    }

    @Test
    public void shouldFailStartIfWaitTimeout()
    {
        try ( GenericContainer container = new GenericContainer<>(TestImages.TINY_IMAGE)
                .withCommand( "tail", "-f", "/dev/null")
                .waitingFor( new ContainerFinishedWaitStrategy().withStartupTimeout( Duration.ofSeconds( 1 ))) ) {
            assertThatThrownBy( container::start ).as( "Should error with timeout when time out" )
                                                  .hasStackTraceContaining( "Timed out waiting for container to finish" );
        }

    }
}
