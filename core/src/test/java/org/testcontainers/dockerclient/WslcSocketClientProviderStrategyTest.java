package org.testcontainers.dockerclient;

import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assumptions.assumeThat;

class WslcSocketClientProviderStrategyTest {

    private final WslcSocketClientProviderStrategy strategy = new WslcSocketClientProviderStrategy();

    @Test
    void resolvesToTheWslcDockerHost() {
        assertThat(strategy.getTransportConfig().getDockerHost()).hasToString("wslc://localhost");
    }

    @Test
    void describesItselfWithTheEndpoint() {
        assertThat(strategy.getDescription()).contains("wslc://localhost");
    }

    @Test
    void sitsBelowTheNamedPipeStrategySoDockerDesktopAlwaysWins() {
        assertThat(strategy.getPriority())
            .isEqualTo(WslcSocketClientProviderStrategy.PRIORITY)
            .isLessThan(NpipeSocketClientProviderStrategy.PRIORITY);
    }

    @Test
    void neverAppliesOffWindows() {
        assertThat(WslcSocketClientProviderStrategy.applies(false)).isFalse();
    }

    @Test
    void probesTheEnvironmentWhenOnWindows() {
        // The outcome depends on whether wslc is installed on the machine running the build, so this
        // pins only what is environment-independent: the probe is reached and completes.
        assertThatNoException().isThrownBy(() -> WslcSocketClientProviderStrategy.applies(true));
    }

    @Test
    void isApplicableCompletesOnAnyPlatform() {
        assertThatNoException().isThrownBy(strategy::isApplicable);
    }

    @Test
    void isNotApplicableOffWindowsEvenIfSomethingCalledWslcExists() {
        assumeThat(SystemUtils.IS_OS_WINDOWS).isFalse();
        assertThat(strategy.isApplicable()).isFalse();
    }

    @Test
    void isNotPersistedSoALaterRunCannotSkipTheNamedPipeStrategy() {
        // A persisted strategy is loaded ahead of the priority-sorted ones, which would let wslc
        // keep winning after a Docker Desktop or Podman pipe becomes available.
        assertThat(strategy.isPersistable()).isFalse();
    }

    @Test
    void skipsTheSocketProbeBecauseThereIsNoConnectableEndpoint() {
        // A stdio bridge has neither a socket file nor a port to connect() to, so test() must defer
        // to the daemon ping in tryOutStrategy rather than warn about an unrecognised scheme.
        assertThat(strategy.test()).isTrue();
    }

    @Test
    void defaultsTheExecutableWhenTheEnvironmentDoesNotNameOne() {
        assertThat(WslcSocketClientProviderStrategy.resolveExecutable(null))
            .isEqualTo(WslcSocketClientProviderStrategy.DEFAULT_EXECUTABLE);
        assertThat(WslcSocketClientProviderStrategy.resolveExecutable("   "))
            .isEqualTo(WslcSocketClientProviderStrategy.DEFAULT_EXECUTABLE);
    }

    @Test
    void honoursAConfiguredExecutable() {
        assertThat(WslcSocketClientProviderStrategy.resolveExecutable("C:\\tools\\wslc.exe"))
            .isEqualTo("C:\\tools\\wslc.exe");
    }

    @Test
    void asksWslcOnlyForItsVersion() {
        assertThat(WslcSocketClientProviderStrategy.probeCommand("wslc.exe", true).command())
            .containsExactly("wslc.exe", "version");
    }

    @Test
    void discardsProbeOutputToThePlatformNullDevice() {
        ProcessBuilder onWindows = WslcSocketClientProviderStrategy.probeCommand("wslc.exe", true);
        ProcessBuilder elsewhere = WslcSocketClientProviderStrategy.probeCommand("wslc", false);

        assertThat(onWindows.redirectOutput().file()).hasName("NUL");
        assertThat(elsewhere.redirectOutput().file()).hasName("null");
        assertThat(onWindows.redirectErrorStream()).isTrue();
    }

    @Test
    void probeSucceedsWhenTheCommandExitsZero() {
        assertThat(WslcSocketClientProviderStrategy.probe(java("-version"), 30_000)).isTrue();
    }

    @Test
    void probeFailsWhenTheCommandExitsNonZero() {
        assertThat(WslcSocketClientProviderStrategy.probe(java("-XXdefinitelyNotAValidFlag"), 30_000)).isFalse();
    }

    @Test
    void probeFailsWhenTheExecutableCannotBeRun() {
        ProcessBuilder missing = new ProcessBuilder("wslc-does-not-exist-" + UUID.randomUUID());

        assertThat(WslcSocketClientProviderStrategy.probe(missing, 30_000)).isFalse();
    }

    @Test
    void probeFailsWhenTheCommandOutlivesTheTimeout() {
        assertThat(WslcSocketClientProviderStrategy.probe(sleeper(), 200)).isFalse();
    }

    @Test
    void probeFailsAndRestoresTheInterruptFlagWhenInterrupted() {
        Thread.currentThread().interrupt();
        try {
            assertThat(WslcSocketClientProviderStrategy.probe(sleeper(), 30_000)).isFalse();
            assertThat(Thread.currentThread().isInterrupted())
                .as("interrupt flag restored rather than swallowed")
                .isTrue();
        } finally {
            // Clear it so the rest of the suite is unaffected.
            Thread.interrupted();
        }
    }

    /**
     * The JVM running the build, which exists on every platform the suite runs on.
     */
    private static ProcessBuilder java(String... args) {
        String executable = SystemUtils.IS_OS_WINDOWS ? "java.exe" : "java";
        List<String> command = new ArrayList<>();
        command.add(Paths.get(System.getProperty("java.home"), "bin", executable).toString());
        command.addAll(Arrays.asList(args));
        return discarding(new ProcessBuilder(command));
    }

    /**
     * A command that outlives any timeout these tests use, so the timeout and interrupt paths are
     * reached deterministically rather than by racing process startup.
     */
    private static ProcessBuilder sleeper() {
        return discarding(
            SystemUtils.IS_OS_WINDOWS
                ? new ProcessBuilder("cmd", "/c", "ping", "-n", "10", "127.0.0.1")
                : new ProcessBuilder("sleep", "5")
        );
    }

    private static ProcessBuilder discarding(ProcessBuilder builder) {
        return builder.redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD);
    }
}
