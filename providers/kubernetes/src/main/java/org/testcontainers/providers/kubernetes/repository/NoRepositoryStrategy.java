package org.testcontainers.providers.kubernetes.repository;

public class NoRepositoryStrategy implements RepositoryStrategy {
    @Override
    public String getRandomImageName() {
        throw new RuntimeException("No image strategy configured."); // TODO: Custom exception? Helpful description!
    }
}
