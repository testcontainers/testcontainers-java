import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.Callable;

/**
 * Created by rnorth on 03/01/2016.
 */
public interface MailSenderService extends Callable<MailSenderService.Result> {

    /**
     * Enqueue a message to be sent on a subsequent {@link DisqueBackedMailSenderService#doScheduledSend()}
     * @param message   mail message to be sent
     * @return          an ID for the enqueued message
     */
    String enqueueMessage(MailMessage message);

    /**
     * Trigger a scheduled send of mail messages.
     *
     * TODO: In a real implementation this could be called by a {@link java.util.concurrent.ScheduledExecutorService},
     * probably
     *
     * @return the result of sending queued messages.
     */
    MailSenderService.Result doScheduledSend();

    /**
     * call() implementation to allow MailSenderService to be used as a Callable. Simply delegates to
     * the {@link DisqueBackedMailSenderService#doScheduledSend()} method.
     *
     * @return the result of sending queued messages.
     * @throws Exception
     */
    default MailSenderService.Result call() throws Exception {
        return doScheduledSend();
    }

    @Data
    @RequiredArgsConstructor
    class Result {
        public final int successfulCount;
        public final int failedCount;
    }
}
