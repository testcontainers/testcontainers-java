package org.testcontainers.controller.intents;


import com.github.dockerjava.api.model.Network;

import java.util.List;

public interface ListNetworksIntent {
    ListNetworksIntent withNameFilter(String nameFilter);

    ListNetworksIntent withIdFilter(String idFilter);

    List<Network> perform(); // TODO: Change return type
}
