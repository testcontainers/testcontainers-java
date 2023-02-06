package org.testcontainers.containers;

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

/**
 * @author Tomasz Forys
 */
public class SolaceContainerRESTTest {

    @Test
    public void testSolaceContainer() throws IOException {
        try (
            SolaceContainer solace = new SolaceContainer(SolaceContainerTestProperties.getImageName())
                .withTopic(SolaceContainerTestProperties.TOPIC_NAME, Service.REST)
                .withVpn("rest-vpn")
        ) {
            solace.start();
            testPublishMessageToSolace(solace, Service.REST);
        }
    }

    private void testPublishMessageToSolace(SolaceContainer solace, Service service) throws IOException {
        HttpClient client = createClient(solace);
        HttpPost request = new HttpPost(solace.getOrigin(service) + "/" + SolaceContainerTestProperties.TOPIC_NAME);
        request.setEntity(new StringEntity(SolaceContainerTestProperties.MESSAGE));
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
