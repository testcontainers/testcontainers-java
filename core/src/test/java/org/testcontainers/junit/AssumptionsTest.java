package org.testcontainers.junit;

import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.assumptions.Assumptions;
import org.testcontainers.junit.assumptions.DockerAvailableAssumptionRule;

public class AssumptionsTest {

    @ClassRule
    public static GenericContainer alpine = new GenericContainer("alpine:3.5");

    @ClassRule
    public static DockerAvailableAssumptionRule assumeDockerPresent = Assumptions.assumeDockerPresent();

    @ClassRule
    public static GenericContainer alpine2 = new GenericContainer("alpine:3.9");

    @Test
    public void testNothing() {
        System.err.println("Here");
    }

    @Test
    public void testNothing2() {
        System.err.println("Here");
    }
}