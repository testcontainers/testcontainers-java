package org.testcontainers.docker;

import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.model.Container;
import com.google.common.collect.ImmutableMap;
import org.testcontainers.controller.intents.ListContainersIntent;

import java.util.List;

public class ListContainersDockerIntent implements ListContainersIntent {

    private final ListContainersCmd listContainersCmd;

    public ListContainersDockerIntent(ListContainersCmd listContainersCmd) {
        this.listContainersCmd = listContainersCmd;
    }

    @Override
    public ListContainersIntent withShowAll(boolean showAll) {
        listContainersCmd.withShowAll(showAll);
        return this;
    }

    @Override
    public ListContainersIntent withLabelFilter(ImmutableMap<String, String> labelFilter) {
        listContainersCmd.withLabelFilter(labelFilter);
        return this;
    }

    @Override
    public ListContainersIntent withLimit(int limit) {
        listContainersCmd.withLimit(limit);
        return this;
    }

    @Override
    public ListContainersIntent withStatusFilter(List<String> statusFilter) {
        listContainersCmd.withStatusFilter(statusFilter);
        return this;
    }

    @Override
    public List<Container> perform() {
        return listContainersCmd.exec();
    }

}
