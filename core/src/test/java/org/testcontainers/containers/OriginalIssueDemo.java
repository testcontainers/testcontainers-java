package org.testcontainers.containers;

import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to demonstrate the fix for the original issue where variable substitution was not working.
 */  
public class OriginalIssueDemo {

    @Test
    public void demonstrateOriginalIssueIsFixed() {
        // Set the environment variable that was mentioned in the issue
        System.setProperty("TAG_CONFLUENT", "7.0.0");
        
        try {
            // Parse the compose file that contains the problematic image name
            ParsedDockerComposeFile parsedFile = new ParsedDockerComposeFile(
                new File("src/test/resources/docker-compose-variable-substitution.yml")
            );
            
            // Before our fix, this would contain "confluentinc/cp-server:${TAG_CONFLUENT}"
            // After our fix, this should contain "confluentinc/cp-server:7.0.0"
            String actualImageName = parsedFile.getServiceNameToImageNames()
                .get("confluent")
                .iterator()
                .next();
                
            assertThat(actualImageName)
                .as("Image name should have variable substituted")
                .isEqualTo("confluentinc/cp-server:7.0.0")
                .doesNotContain("${TAG_CONFLUENT}");
                
            System.out.println("✅ SUCCESS: Variable substitution is working!");
            System.out.println("   Original: confluentinc/cp-server:${TAG_CONFLUENT}");
            System.out.println("   Resolved: " + actualImageName);
            
        } finally {
            System.clearProperty("TAG_CONFLUENT");
        }
    }
}