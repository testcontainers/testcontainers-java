import biz.paluch.spinach.DisqueClient;
import biz.paluch.spinach.DisqueURI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.context;
import static org.rnorth.visibleassertions.VisibleAssertions.info;

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
        context("");
        disqueClient = new DisqueClient(DisqueURI.create(container.getContainerIpAddress(), container.getMappedPort(7711)));
        mockMailApiClient = mock(MailApiClient.class);

        service = new DisqueBackedMailSenderService(disqueClient, mockMailApiClient);

        info("Initialized a fresh Disque instance and service instance");

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

        info("Message 1 will fail to send on the first attempt");
        when(mockMailApiClient.send(eq(dummyMessage1))).thenThrow(MailApiClient.MailSendException.class).thenReturn(true);
        when(mockMailApiClient.send(eq(dummyMessage2))).thenReturn(true);
        when(mockMailApiClient.send(eq(dummyMessage3))).thenReturn(true);

        MailSenderService.Result result;
        context("Simulating sending messages");
        context("First sending attempt", 4);
        result = service.doScheduledSend();
        assertEquals("2 messages were 'sent' successfully", 2, result.successfulCount);
        assertEquals("1 messages failed", 1, result.failedCount);

        context("Second attempt", 4);
        result = service.doScheduledSend();
        assertEquals("1 message was 'sent' successfully", 1, result.successfulCount);
        assertEquals("0 messages failed", 0, result.failedCount);

        context("Third attempt", 4);
        info("No messages should be due to send this time");
        result = service.doScheduledSend();
        assertEquals("0 messages were 'sent' successfully", 0, result.successfulCount);
        assertEquals("0 messages failed", 0, result.failedCount);

        verify(mockMailApiClient, times(2)).send(eq(dummyMessage1));
        verify(mockMailApiClient).send(eq(dummyMessage2));
        verify(mockMailApiClient).send(eq(dummyMessage3));
    }
}
