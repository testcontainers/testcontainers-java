package org.testcontainers.controller;


import java.util.List;
import java.util.Map;

public interface ResourceCleaner {
    void stopAndRemoveContainer(String containerId, String imageName); // TODO: Rename to ResourceReaper once the original has been migrated

    void registerFilterForCleanup(List<Map.Entry<String, String>> label);

    void removeNetworkById(String id);

    void registerImageForCleanup(String imageReference);

    String start(); // TODO: Does this belong here (only called from DockerClientFactory), might be provider specific

    void registerContainerForCleanup(String containerId);
}
