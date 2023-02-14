package org.testcontainers.solace;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class SolaceContainerRESTTest {

    private static final String MESSAGE = "HelloWorld";

    private static final String TOPIC_NAME = "Topic/ActualTopic";

    @Test
    public void testSolaceContainer() throws IOException {
        try (
            SolaceContainer solaceContainer = new SolaceContainer("solace/solace-pubsub-standard:10.2")
                .withTopic(TOPIC_NAME, Service.REST)
                .withVpn("rest-vpn")
        ) {
            solaceContainer.start();
            testPublishMessageToSolace(solaceContainer, Service.REST);
        }
    }

    private void testPublishMessageToSolace(SolaceContainer solaceContainer, Service service) throws IOException {
        HttpClient client = createClient(solaceContainer);
        HttpPost request = new HttpPost(solaceContainer.getOrigin(service) + "/" + TOPIC_NAME);
        request.setEntity(new StringEntity(MESSAGE));
        request.addHeader(HttpHeaders.CONTENT_TYPE, "text/plain");
        HttpResponse response = client.execute(request);
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            Assert.fail("Cannot send message to solace - " + EntityUtils.toString(response.getEntity()));
        }
        Assertions.assertThat(EntityUtils.toString(response.getEntity())).isEmpty();
    }

    private HttpClient createClient(SolaceContainer solaceContainer) {
        CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(
            AuthScope.ANY,
            new UsernamePasswordCredentials(solaceContainer.getUsername(), solaceContainer.getPassword())
        );
        return HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
    }
}
