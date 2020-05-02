/**
 * Created by rnorth on 03/01/2016.
 */
public interface MailApiClient {

    boolean send(MailMessage message) throws MailSendException;

    class MailSendException extends RuntimeException {

    }
}
