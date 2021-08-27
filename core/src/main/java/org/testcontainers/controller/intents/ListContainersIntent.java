package org.testcontainers.controller.intents;

import com.github.dockerjava.api.model.Container;
import com.google.common.collect.ImmutableMap;

import java.util.List;

public interface ListContainersIntent {

    ListContainersIntent withShowAll(boolean showAll);

    ListContainersIntent withLabelFilter(ImmutableMap<String, String> labelFilter);

    ListContainersIntent withLimit(int limit);

    ListContainersIntent withStatusFilter(List<String> statusFilter);

    List<Container> perform(); // TODO: Change return type

}
