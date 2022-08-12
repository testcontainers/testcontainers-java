import biz.paluch.spinach.DisqueClient;
import biz.paluch.spinach.DisqueURI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by rnorth on 03/01/2016.
 */
public class DisqueBackedMailSenderTest {

    @Rule
    public GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("richnorth/disque:1.0-rc1"))
                                                .withExposedPorts(7711);
    private DisqueClient disqueClient;

    private MailSenderService service;
    private MailApiClient mockMailApiClient;

    private MailMessage dummyMessage1;
    private MailMessage dummyMessage2;
    private MailMessage dummyMessage3;

    @Before
    public void setup() {
        disqueClient = new DisqueClient(DisqueURI.create(container.getHost(), container.getMappedPort(7711)));
        mockMailApiClient = mock(MailApiClient.class);

        service = new DisqueBackedMailSenderService(disqueClient, mockMailApiClient);

        dummyMessage1 = new MailMessage("Dummy Message 1", "bob@example.com", "Test body");
        dummyMessage2 = new MailMessage("Dummy Message 2", "bob@example.com", "Test body");
        dummyMessage3 = new MailMessage("Dummy Message 3", "bob@example.com", "Test body");
    }

    @Test
    public void testSimpleSuccessfulSending() throws Exception {
        service.enqueueMessage(dummyMessage1);
        service.enqueueMessage(dummyMessage2);
        service.enqueueMessage(dummyMessage3);

        when(mockMailApiClient.send(any(MailMessage.class))).thenReturn(true);

        service.doScheduledSend();

        verify(mockMailApiClient).send(eq(dummyMessage1));
        verify(mockMailApiClient).send(eq(dummyMessage2));
        verify(mockMailApiClient).send(eq(dummyMessage3));
    }

    @Test
    public void testRetryOnFailure() throws Exception {
        service.enqueueMessage(dummyMessage1);
        service.enqueueMessage(dummyMessage2);
        service.enqueueMessage(dummyMessage3);

        when(mockMailApiClient.send(eq(dummyMessage1))).thenThrow(MailApiClient.MailSendException.class).thenReturn(true);
        when(mockMailApiClient.send(eq(dummyMessage2))).thenReturn(true);
        when(mockMailApiClient.send(eq(dummyMessage3))).thenReturn(true);

        MailSenderService.Result result;

        result = service.doScheduledSend();
        assertThat(result.successfulCount).as("2 messages were 'sent' successfully").isEqualTo(2);
        assertThat(result.failedCount).as("1 messages failed").isEqualTo(1);

        result = service.doScheduledSend();
        assertThat(result.successfulCount).as("1 message was 'sent' successfully").isEqualTo(1);
        assertThat(result.failedCount).as("0 messages failed").isZero();

        result = service.doScheduledSend();
        assertThat(result.successfulCount).as("0 messages were 'sent' successfully").isZero();
        assertThat(result.failedCount).as("0 messages failed").isZero();

        verify(mockMailApiClient, times(2)).send(eq(dummyMessage1));
        verify(mockMailApiClient).send(eq(dummyMessage2));
        verify(mockMailApiClient).send(eq(dummyMessage3));
    }
}
