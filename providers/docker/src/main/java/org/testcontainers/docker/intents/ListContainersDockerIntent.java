package org.testcontainers.docker.intents;

import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.model.Container;
import org.testcontainers.controller.intents.ListContainersIntent;

import java.util.List;
import java.util.Map;

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
    public ListContainersIntent withLabelFilter(Map<String, String> labelFilter) {
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
