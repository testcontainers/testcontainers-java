package org.testcontainers.docker.intents;

import com.github.dockerjava.api.command.ListNetworksCmd;
import com.github.dockerjava.api.model.Network;
import org.testcontainers.controller.intents.ListNetworksIntent;

import java.util.List;

public class ListNetworksDockerIntent implements ListNetworksIntent {
    private final ListNetworksCmd listNetworksCmd;

    public ListNetworksDockerIntent(ListNetworksCmd listNetworksCmd) {
        this.listNetworksCmd = listNetworksCmd;
    }

    @Override
    public ListNetworksIntent withNameFilter(String nameFilter) {
        listNetworksCmd.withNameFilter(nameFilter);
        return this;
    }

    @Override
    public ListNetworksIntent withIdFilter(String idFilter) {
        listNetworksCmd.withIdFilter(idFilter);
        return this;
    }

    @Override
    public List<Network> perform() {
        return listNetworksCmd.exec();
    }
}
