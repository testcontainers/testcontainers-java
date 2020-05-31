package org.testcontainers.containers.localstack;

import org.junit.ClassRule;
import org.junit.Test;

import static org.rnorth.visibleassertions.VisibleAssertions.assertNotEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

public class LocalstackLegacyContainerTest {

    @ClassRule
    public static LocalStackContainer localstack = new LocalStackContainer("0.10.8")
        .withServices(S3, SQS);

    @Test
    public void differentPortsAreExposed() {
        assertTrue("Multiple ports are exposed", localstack.getExposedPorts().size() > 1);
        assertNotEquals(
            "Endpoint overrides are different",
            localstack.getEndpointOverride(S3).toString(),
            localstack.getEndpointOverride(SQS).toString());
        assertNotEquals(
            "Endpoint configuration have different endpoints",
            localstack.getEndpointConfiguration(S3).getServiceEndpoint(),
            localstack.getEndpointConfiguration(SQS).getServiceEndpoint());
    }

}
