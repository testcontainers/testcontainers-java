package org.testcontainers;


import org.junit.Test;
import org.testcontainers.controller.ContainerProvider;
import org.testcontainers.controller.NoSuchProviderException;
import org.testcontainers.utility.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

public class ContainerProviderElectorTest  {

    @Test
    public void test() throws NoSuchProviderException {
        ContainerProviderElector elector = new ContainerProviderElector(TestcontainersConfiguration.getInstance());
        ContainerProvider provider = elector.elect();

        assertThat(provider).isNotNull();
    }


}
