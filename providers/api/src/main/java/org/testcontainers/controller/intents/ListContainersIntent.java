package org.testcontainers.controller.intents;

import com.github.dockerjava.api.model.Container;

import java.util.List;
import java.util.Map;

public interface ListContainersIntent {

    ListContainersIntent withShowAll(boolean showAll);

    ListContainersIntent withLabelFilter(Map<String, String> labelFilter);

    ListContainersIntent withLimit(int limit);

    ListContainersIntent withStatusFilter(List<String> statusFilter);

    List<Container> perform(); // TODO: Change return type

}
