import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.ClassRule;
import org.junit.Test;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

/**
 * Created by rnorth on 18/02/2016.
 */
public class SimpleResourceTest {

    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new JerseyResource())
            .build();

    @Test
    public void simpleTest() {
        assertEquals("the response from the resource",
                "bar",
                resources.client().target("/foo").request().get(String.class));
    }
}
