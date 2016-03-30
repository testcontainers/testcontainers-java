package org.testcontainers.junit;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

/**
 * Failing test for manual testing of container cleanup.
 */
public class TestBadCleanup {
   @Rule
   public GenericContainer failingContainer =
         new GenericContainer("alpine:3.2")
               .withCommand("/bin/sh","-c","false")
               .withExposedPorts(8080);   

   @Test @Ignore
   public void testBadCleanup() throws Exception {
      // no op
      // this test will pass, but a "docker ps -a" afterward will see two leftover containers.
   }
}