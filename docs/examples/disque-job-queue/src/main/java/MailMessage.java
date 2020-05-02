import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by rnorth on 03/01/2016.
 */
@Data @AllArgsConstructor
public class MailMessage {
    public final String subject;
    public final String recipient;
    public final String body;
}
