package org.testcontainers.azure;

import org.junit.jupiter.api.Test;
import org.testcontainers.utility.MountableFile;

import static org.assertj.core.api.Assertions.assertThat;

class AzuriteContainerCommandTest {

    private static final String IMAGE = "mcr.microsoft.com/azure-storage/azurite:3.33.0";

    @Test
    void commandLineOmitsExtraOptionsByDefault() {
        AzuriteContainer emulator = new AzuriteContainer(IMAGE);

        assertThat(emulator.getCommandLine()).doesNotContain("--skipApiVersionCheck");
    }

    @Test
    void commandLineIncludesExtraOptions() {
        // commandOptions {
        AzuriteContainer emulator = new AzuriteContainer("mcr.microsoft.com/azure-storage/azurite:3.33.0")
            .withCommandOptions("--skipApiVersionCheck");
        // }

        assertThat(emulator.getCommandLine())
            .startsWith("azurite --blobHost 0.0.0.0 --queueHost 0.0.0.0 --tableHost 0.0.0.0")
            .endsWith("--skipApiVersionCheck");
    }

    @Test
    void commandLineAppendsMultipleOptions() {
        AzuriteContainer emulator = new AzuriteContainer(IMAGE)
            .withCommandOptions("--skipApiVersionCheck", "--disableProductStyleUrl");

        assertThat(emulator.getCommandLine()).endsWith("--skipApiVersionCheck --disableProductStyleUrl");
    }

    @Test
    void commandLineKeepsExtraOptionsTogetherWithSsl() {
        AzuriteContainer emulator = new AzuriteContainer(IMAGE)
            .withSsl(MountableFile.forClasspathResource("/keystore.pfx"), "changeit")
            .withCommandOptions("--skipApiVersionCheck");

        assertThat(emulator.getCommandLine())
            .contains("--cert /cert.pfx")
            .endsWith("--pwd changeit --skipApiVersionCheck");
    }

    @Test
    void configureAppliesCommandOptionsEvenIfWithCommandWasUsed() {
        AzuriteContainer emulator = new AzuriteContainer(IMAGE)
            .withCommand("azurite --ignored")
            .withCommandOptions("--skipApiVersionCheck");

        emulator.configure();

        assertThat(String.join(" ", emulator.getCommandParts()))
            .isEqualTo("azurite --blobHost 0.0.0.0 --queueHost 0.0.0.0 --tableHost 0.0.0.0 --skipApiVersionCheck");
        assertThat(emulator.getCommandLine()).contains("--blobHost 0.0.0.0").contains("--skipApiVersionCheck");
    }
}
