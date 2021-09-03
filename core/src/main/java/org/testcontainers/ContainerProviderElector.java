package org.testcontainers;

import org.testcontainers.controller.ContainerProvider;
import org.testcontainers.controller.NoSuchProviderException;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.IntFunction;

public class ContainerProviderElector {

    private final TestcontainersConfiguration testcontainersConfiguration;

    public ContainerProviderElector(
        TestcontainersConfiguration testcontainersConfiguration
    ) {
        this.testcontainersConfiguration = testcontainersConfiguration;
    }


    public ContainerProvider elect() throws NoSuchProviderException {
        List<ContainerProvider> providers = new ArrayList<>();
        ServiceLoader
            .load(ContainerProvider.class)
            .forEach(providers::add);

        Optional<String> providerIdentifier = testcontainersConfiguration.getProviderIdentifier();

        if (providerIdentifier.isPresent()) {
            String identifier = providerIdentifier.get();
            return providers.stream()
                .filter(p -> identifier.equalsIgnoreCase(p.getIdentifier()))
                .findFirst()
                .orElseThrow(
                    () -> new NoSuchProviderException(
                        String.format("No such provider: %s", providerIdentifier.get())
                    )
                );
        }

        return providers
            .stream()
            .filter(p -> p.isAvailable())
            .findFirst()
            .orElseThrow(
                () -> new NoSuchProviderException(
                    String.format(
                        "No provider is available fpr this environment (checked: %s).",
                        String.join(
                            ", ",
                            providers.stream().map(ContainerProvider::getIdentifier).toArray(i -> new String[i])
                        )
                    )
                )
            );

    }

}
