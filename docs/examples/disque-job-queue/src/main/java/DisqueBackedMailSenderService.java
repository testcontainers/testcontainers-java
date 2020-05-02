import biz.paluch.spinach.DisqueClient;
import biz.paluch.spinach.api.Job;
import biz.paluch.spinach.api.sync.DisqueCommands;
import com.google.gson.Gson;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Simple example of how a mail service can be backed by a Disque job queue.
 *
 * It's assumed that for actual sending (SMTP, HTTP API, etc), this service calls a {@link MailApiClient}.
 * However, this can be expected to be quite unreliable, so use of Disque as a backing store helps keep track
 * of sent/failed state.
 */
public class DisqueBackedMailSenderService implements MailSenderService {

    private final DisqueCommands<String, String> disque;
    private final Gson gson;
    private final MailApiClient mailApiClient;

    public DisqueBackedMailSenderService(DisqueClient client, MailApiClient mailApiClient) {
        this.mailApiClient = mailApiClient;

        // Obtain a Disque connection from DisqueClient
        disque = client.connect().sync();

        gson = new Gson();
    }

    @Override
    public String enqueueMessage(MailMessage message) {
        String messageAsJson = gson.toJson(message);

        // Enqueue the message as JSON, setting a TTL of 1 day (will expire off the queue if not sent by then)
        String jobId = disque.addjob("mail", messageAsJson, 1, TimeUnit.DAYS);

        return jobId;
    }

    @Override
    public Result doScheduledSend() {

        Set<String> succeededJobIds = new HashSet<>();
        Set<String> failedJobIds = new HashSet<>();

        // Wait up to 100ms for new messages to arrive on the queue
        // Retrieve up to 10 messages
        // (Ordinarily we'd make these configurable!)
        List<Job<String, String>> jobs = disque.getjobs(100, TimeUnit.MILLISECONDS, 10, "mail");

        for (Job<String, String> job : jobs) {
            String jsonBody = job.getBody();
            MailMessage message = gson.fromJson(jsonBody, MailMessage.class);

            try {
                mailApiClient.send(message);
                succeededJobIds.add(job.getId());
            } catch (MailApiClient.MailSendException e) {
                failedJobIds.add(job.getId());
            }
        }

        // For any failed messages, proactively return to the queue
        disque.nack(failedJobIds.toArray(new String[failedJobIds.size()]));
        // For any successful jobs, mark as completed
        disque.ackjob(succeededJobIds.toArray(new String[succeededJobIds.size()]));

        return new Result(succeededJobIds.size(), failedJobIds.size());
    }
}
