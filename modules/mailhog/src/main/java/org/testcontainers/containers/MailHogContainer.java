package org.testcontainers.containers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.testcontainers.containers.model.Mail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Testcontainer for MailHog (https://github.com/mailhog/MailHog) which is an email testing tool.
 * The docker image used is mailhog/mailhog.
 *
 * The internal http and smtp port of mailhog to use from other containers in the same docker
 * network are {@link #HTTP_PORT} and {@link #SMTP_PORT}.
 */
public class MailHogContainer extends GenericContainer<MailHogContainer> {

    private static final String VERSION = "v1.0.1";

    public static final int SMTP_PORT = 1025;
    public static final int HTTP_PORT = 8025;

    private static final String ANY_ADDRESS = "0.0.0.0";
    private static final String MAILHOG_QUERY_PARAMETER_LIMIT = "limit";
    private static final String MAILHOG_QUERY_PARAMETER_KIND = "kind";
    private static final String MAILHOG_QUERY_PARAMETER_QUERY = "query";

    public static final String MAIL_HEADER_SUBJECT = "Subject";
    public static final String MAIL_HEADER_DATE = "Date";
    public static final String MAIL_HEADER_TO = "To";
    public static final String MAIL_HEADER_CC = "Cc";

    /**
     * Creates a test container with the latest available docker container for mailhog.
     */
    public MailHogContainer() {
        this(VERSION);
    }

    /**
     * Creates a test container for mailhog with the given version
     * @param version The version of the docker container for mailhog
     */
    public MailHogContainer(String version) {
        super("mailhog/mailhog:" + version);
        withEnv("MH_SMTP_BIND_ADDR", ANY_ADDRESS + ":" + SMTP_PORT);
        withEnv("MH_UI_BIND_ADDR", ANY_ADDRESS + ":" + HTTP_PORT);
        withEnv("MH_API_BIND_ADDR", ANY_ADDRESS + ":" + HTTP_PORT);
        withExposedPorts(SMTP_PORT, HTTP_PORT);
    }

    /**
     * Returns the external http endpoint
     * @return endpoint
     */
    public String getHttpEndpoint() {
        return String.format("http://%s:%d", getContainerIpAddress(), getHttpPort());
    }

    /**
     * Returns the external SMTP endpoint
     * @return endpoint
     */
    public String getSmtpEndpoint() {
        return String.format("%s:%d", getContainerIpAddress(), getSmtpPort());
    }

    /**
     * Returns the external http port
     * @return port
     */
    public int getHttpPort() {
        return getMappedPort(HTTP_PORT);
    }

    /**
     * Returns the external smtp port
     * @return port
     */
    public int getSmtpPort() {
        return getMappedPort(SMTP_PORT);
    }

    /**
     * @return all Mails
     * @throws IOException
     * @throws URISyntaxException
     */
    public List<Mail> getAllMails() throws IOException, URISyntaxException {
        return getMails(Integer.MAX_VALUE);
    }

    /**
     * Returns the newest Mails with the given limit
     * @param limit
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
    public List<Mail> getMails(int limit) throws IOException, URISyntaxException {
        return getMailsWithParameters(new BasicNameValuePair(MAILHOG_QUERY_PARAMETER_LIMIT, Integer.toString(limit)));
    }

    /**
     * Returns the newest Mails from specific sender with given limit
     * @param sender
     * @param limit
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    public List<Mail> getMailsFrom(String sender, int limit) throws URISyntaxException, IOException {
        return getMailsWithParameters(
            new BasicNameValuePair(MAILHOG_QUERY_PARAMETER_KIND, "from"),
            new BasicNameValuePair(MAILHOG_QUERY_PARAMETER_QUERY, sender),
            new BasicNameValuePair(MAILHOG_QUERY_PARAMETER_LIMIT, Integer.toString(limit))
        );
    }

    /**
     * Returns the newest Mail from given sender
     * @param sender
     * @return optional of the mail, empty optional if no Mail from the sender is found
     * @throws URISyntaxException
     * @throws IOException
     */
    public Optional<Mail> getNewestMailFrom(String sender) throws URISyntaxException, IOException {
        return getMailsFrom(sender, 1).stream().findFirst();
    }

    /**
     * Returns all mails from given sender.
     * @param sender
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
    public List<Mail> getAllMailsFrom(String sender) throws IOException, URISyntaxException {
        return getMailsFrom(sender, Integer.MAX_VALUE);
    }

    /**
     * The mails queried with the given parameter
     * (see <a href="https://github.com/blueimp/mailhog/blob/master/docs/APIv2/swagger-2.0.yaml">swagger documentation of mailhog</a>)
     * @param parameters
     * @return list of mails
     * @throws URISyntaxException
     * @throws IOException
     */
    public List<Mail> getMailsWithParameters(NameValuePair... parameters) throws URISyntaxException, IOException {
        ObjectMapper mapper = new ObjectMapper();

        URIBuilder uri = new URIBuilder(getHttpEndpoint());

        boolean isSearch = Arrays.stream(parameters)
            .map(NameValuePair::getName)
            .anyMatch(name -> Arrays.asList(MAILHOG_QUERY_PARAMETER_KIND, MAILHOG_QUERY_PARAMETER_QUERY).contains(name));

        if (isSearch) {
            uri.setPath("/api/v2/search");
        } else {
            uri.setPath("/api/v2/messages");
        }

        uri.setParameters(parameters);

        JsonNode jsonNode = mapper.readTree(uri.build().toURL());
        return Arrays.asList(mapper.treeToValue(jsonNode.get("items"), Mail[].class));
    }

}
