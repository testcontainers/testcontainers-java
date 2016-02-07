import com.sun.jersey.api.client.Client;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

/**
 * Created by rnorth on 14/01/2016.
 */
public class OldJerseyClientTest {

    @Rule
    public GenericContainer httpd = new GenericContainer("httpd:2.4").withExposedPorts(80);

    @Test
    public void clientRequestTest() {
        Client client = Client.create();
        String s = client.resource("http://" + httpd.getContainerIpAddress() + ":" + httpd.getMappedPort(80)).get(String.class);

        assertTrue("httpd is displaying its default placeholder page", s.contains("It works"));
    }
}
