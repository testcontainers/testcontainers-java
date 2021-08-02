package org.testcontainers.containers;

import static java.time.temporal.ChronoUnit.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.http.HttpStatus;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.model.Mail;

import com.google.common.collect.Lists;

public class MailHogContainerTest {

    private static final String DEFAULT_RECIPIENT = "fooRecipient@foo.com";
    private static final String DEFAULT_SENDER = "fooSender@foo.com";

    // createContainer {
    @Rule
    public MailHogContainer mailHog = new MailHogContainer();
    // }
    private Session session;

    @Before
    public void init() {
        Properties prop = new Properties();
        prop.put("mail.smtp.host", mailHog.getContainerIpAddress());
        prop.put("mail.smtp.port", mailHog.getSmtpPort());

        session = Session.getInstance(prop);
    }

    @Test
    public void testHttpResponse() {
        try {
            URL url = new URL(mailHog.getHttpEndpoint());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            MatcherAssert.assertThat("HTTP response code is not 200",
                connection.getResponseCode(),
                is(HttpStatus.SC_OK));
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }

    @Test
    public void testMailSendSuccessful() throws MessagingException, IOException, URISyntaxException {
        String mail = "Mail text";
        String subject = "my subject";
        sendMail(DEFAULT_SENDER, subject, mail);

        List<Mail> mails = mailHog.getAllMails();
        assertThat(mails.size(), is(1));
        assertThat(mails.get(0).getContent().getBody(), is(mail));
        assertThat(mails.get(0).getSubject(), is(subject));
        assertThat(mails.get(0).getDate(), notNullValue());
    }

    @Test
    public void testMailDate() throws MessagingException, IOException, URISyntaxException {
        String mail = "Mail text";
        String subject = "my subject";
        ZonedDateTime before = ZonedDateTime.now().truncatedTo(SECONDS);
        sendMail(DEFAULT_SENDER, subject, mail);
        ZonedDateTime after = ZonedDateTime.now().truncatedTo(SECONDS);


        List<Mail> mails = mailHog.getAllMails();
        ZonedDateTime date = mails.get(0).getDate();

        assertThat(date.isBefore(after) || date.isEqual(after), is(true));
        assertThat(date.isAfter(before) || date.isEqual(before), is(true));
    }

    @Test
    public void testGetNewestMailFrom() throws MessagingException, IOException, URISyntaxException {
        String mail = "Mail text";
        String subject1 = "subject1";
        String subject2 = "subject2";
        String subject3 = "subject3";
        String subject31 = "subject31";
        String subject4 = "subject4";
        sendMail(DEFAULT_SENDER, subject1, mail);
        sendMail("foo2@foo.com", subject2, mail);
        String senderMailAddress = "foo3@foo.com";
        sendMail(senderMailAddress, subject31, mail);
        sendMail(senderMailAddress, subject3, mail);
        sendMail("foo4@foo.com", subject4, mail);

        // exampleGetMail {
        // Query MailHog API to retrieve a single mail
        Optional<Mail> mails = mailHog.getNewestMailFrom(senderMailAddress);
        assertThat(mails.isPresent(), is(true));
        assertThat(mails.get().getSubject(), is(subject3));
        // }
    }

    @Test
    public void testTo() throws MessagingException, IOException, URISyntaxException {
        String mail = "Mail text";
        String subject = "my subject";
        String[] additionalRecipients = {"foo1@foo.com", "Foo foobar <foo3@foo.com>"};
        sendMail(DEFAULT_SENDER, subject, mail, additionalRecipients);

        // exampleGetAllMails {
        List<Mail> mails = mailHog.getAllMails();
        // }

        assertThat(mails.get(0).getTo(), is(Lists.asList(DEFAULT_RECIPIENT, additionalRecipients)));
    }

    @Test
    public void testCC() throws MessagingException, IOException, URISyntaxException {
        String mail = "Mail text";
        String subject = "my subject";
        List<String> cc = Lists.newArrayList("foo1cc@foo.com", "foo3cc@foo.com");
        sendMail(subject, mail, cc);

        List<Mail> mails = mailHog.getAllMails();

        assertThat(mails.get(0).getCC(), is(cc));
    }

    @Test
    public void testGetMailFromSender() throws MessagingException, IOException, URISyntaxException {
        String sender1 = "sender1@foo.com";
        String sender2 = "sender2@foo.com";
        String subject1 = "Test subject1";
        String subject2 = "Test subject2";
        String mail1 = "Test mail1";
        String mail2 = "Test mail2";

        sendMail(sender1, subject1, mail1);
        sendMail(sender1, subject1, mail1);
        sendMail(sender2, subject2, mail2);
        sendMail(sender1, subject1, mail1);

        List<Mail> mails = mailHog.getAllMailsFrom(sender2);
        assertThat(mails.size(), is(1));
        assertThat(mails.get(0).getContent().getBody(), is(mail2));
        assertThat(mails.get(0).getSubject(), is(subject2));

        List<Mail> mailItemsSender2 = mailHog.getAllMailsFrom(sender1);
        assertThat(mailItemsSender2.size(), is(3));
    }

    @Test
    public void testGetMailFromSenderWithName() throws MessagingException, IOException, URISyntaxException {
        String sender1 = "sender1@foo.com";
        String sender2 = "Foobar Foo <sender2@foo.com>";
        String subject1 = "Test subject1";
        String subject2 = "Test subject2";
        String mail1 = "Test mail1";
        String mail2 = "Test mail2";

        sendMail(sender1, subject1, mail1);
        sendMail(sender1, subject1, mail1);
        sendMail(sender2, subject2, mail2);
        sendMail(sender1, subject1, mail1);

        List<Mail> mails = mailHog.getAllMailsFrom("sender2@foo.com");
        assertThat(mails.size(), is(1));
        assertThat(mails.get(0).getContent().getBody(), is(mail2));
        assertThat(mails.get(0).getSubject(), is(subject2));
    }

    private void sendMail(String subject, String mail, List<String> cc) throws MessagingException {
        sendMail(DEFAULT_SENDER, subject, mail, cc);
    }

    private void sendMail(String sender, String subject, String mail, List<String> cc, String... additionalRecipients) throws MessagingException {
        Message mimeMail = new MimeMessage(session);
        mimeMail.setRecipient(Message.RecipientType.TO, new InternetAddress(DEFAULT_RECIPIENT));

        for (String recipient : additionalRecipients) {
            mimeMail.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
        }

        for (String ccReceiver : cc) {
            mimeMail.addRecipient(Message.RecipientType.CC, new InternetAddress(ccReceiver));
        }

        mimeMail.setFrom(new InternetAddress(sender));
        mimeMail.setSubject(subject);
        mimeMail.setText(mail);

        Transport.send(mimeMail);
    }

    private void sendMail(String sender, String subject, String mail, String... additionalRecipients) throws MessagingException {
        sendMail(sender, subject, mail, new ArrayList<>(), additionalRecipients);
    }

}
