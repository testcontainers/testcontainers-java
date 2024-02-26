package org.testcontainers.openfga;

import dev.openfga.sdk.api.client.OpenFgaClient;
import dev.openfga.sdk.api.client.model.ClientCreateStoreResponse;
import dev.openfga.sdk.api.configuration.ClientConfiguration;
import dev.openfga.sdk.api.model.CreateStoreRequest;
import dev.openfga.sdk.errors.FgaInvalidParameterException;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class OpenFGAContainerTest {

    @Test
    public void withDefaultConfig() throws FgaInvalidParameterException, ExecutionException, InterruptedException {
        try ( // container
            OpenFGAContainer openfga = new OpenFGAContainer("openfga/openfga:v1.4.3")
            // }
        ) {
            openfga.start();

            ClientConfiguration config = new ClientConfiguration().apiUrl(openfga.getHttpEndpoint());
            OpenFgaClient client = new OpenFgaClient(config);

            assertThat(client.listStores().get().getStores()).isEmpty();
            ClientCreateStoreResponse store = client.createStore(new CreateStoreRequest().name("test")).get();
            assertThat(store.getId()).isNotNull();
            assertThat(client.listStores().get().getStores()).hasSize(1);
        }
    }
}
