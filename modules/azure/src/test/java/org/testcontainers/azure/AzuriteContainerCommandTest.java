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
        AzuriteContainer emulator = new AzuriteContainer(IMAGE).withCommandOptions("--skipApiVersionCheck");
        // }

        assertThat(emulator.getCommandLine())
            .startsWith("azurite --blobHost 0.0.0.0 --queueHost 0.0.0.0 --tableHost 0.0.0.0")
            .contains("--skipApiVersionCheck");
    }

    @Test
    void commandLineAppendsMultipleOptions() {
        AzuriteContainer emulator = new AzuriteContainer(IMAGE)
            .withCommandOptions("--skipApiVersionCheck", "--disableProductStyleUrl");

        assertThat(emulator.getCommandLine()).contains("--skipApiVersionCheck").contains("--disableProductStyleUrl");
    }

    @Test
    void commandLineKeepsExtraOptionsTogetherWithSsl() {
        AzuriteContainer emulator = new AzuriteContainer(IMAGE)
            .withSsl(MountableFile.forClasspathResource("/keystore.pfx"), "changeit")
            .withCommandOptions("--skipApiVersionCheck");

        assertThat(emulator.getCommandLine())
            .contains("--cert /cert.pfx")
            .contains("--pwd changeit")
            .contains("--skipApiVersionCheck");
    }

    @Test
    void configureAppliesCommandOptionsEvenIfWithCommandWasUsed() {
        AzuriteContainer emulator = new AzuriteContainer(IMAGE)
            .withCommand("azurite --skipApiVersionCheck")
            .withCommandOptions("--skipApiVersionCheck");

        emulator.configure();

        assertThat(String.join(" ", emulator.getCommandParts())).contains("--skipApiVersionCheck");
        assertThat(emulator.getCommandLine()).contains("--blobHost 0.0.0.0").contains("--skipApiVersionCheck");
    }
}
