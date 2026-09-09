package org.testcontainers.dockerclient;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * Auto-detects a WSL Containers (wslc) Docker daemon when no {@code DOCKER_HOST} is configured and no
 * real Docker/Podman endpoint is available. wslc exposes neither a Windows named pipe nor a TCP port;
 * its daemon is reached through a stdio bridge (see the {@code wslc://} docker-java transport).
 * <p>
 * The priority is below {@link NpipeSocketClientProviderStrategy} so an existing Docker Desktop or
 * Podman named pipe always wins; wslc is only used as a fallback, and only on Windows when the
 * {@code wslc} CLI is actually present.
 *
 * @deprecated this class is used by the SPI and should not be used directly
 */
@Slf4j
@Deprecated
public final class WslcSocketClientProviderStrategy extends DockerClientProviderStrategy {

    private static final String SOCKET_LOCATION = "wslc://localhost";

    public static final int PRIORITY = NpipeSocketClientProviderStrategy.PRIORITY - 10;

    static final String WSLC_EXECUTABLE_ENV = "WSLC_EXECUTABLE";

    static final String DEFAULT_EXECUTABLE = "wslc.exe";

    static final long PROBE_TIMEOUT_MS = 10_000;

    @Override
    public TransportConfig getTransportConfig() {
        return TransportConfig.builder().dockerHost(URI.create(SOCKET_LOCATION)).build();
    }

    @Override
    protected boolean isApplicable() {
        return applies(SystemUtils.IS_OS_WINDOWS);
    }

    @Override
    public String getDescription() {
        return "WSL Containers (wslc) session (" + SOCKET_LOCATION + ")";
    }

    @Override
    protected int getPriority() {
        return PRIORITY;
    }

    /**
     * Never remembered in {@code ~/.testcontainers.properties}. A persisted strategy is loaded ahead
     * of the priority-sorted ones by {@code getFirstValidStrategy}, so recording this one would let
     * wslc keep winning on later runs even once a Docker Desktop or Podman named pipe is available —
     * defeating the priority that is supposed to keep it a fallback.
     */
    @Override
    protected boolean isPersistable() {
        return false;
    }

    /**
     * Split from {@link #isApplicable()} so the Windows gate can be exercised on any platform: the
     * OS cannot be varied at runtime.
     */
    static boolean applies(boolean windows) {
        return windows && isWslcAvailable();
    }

    private static boolean isWslcAvailable() {
        String executable = resolveExecutable(System.getenv(WSLC_EXECUTABLE_ENV));
        return probe(probeCommand(executable, SystemUtils.IS_OS_WINDOWS), PROBE_TIMEOUT_MS);
    }

    /**
     * @return {@link #DEFAULT_EXECUTABLE} unless {@code configured} names something; a blank
     *         {@code WSLC_EXECUTABLE} is treated as unset rather than passed on to fail as a
     *         missing executable
     */
    static String resolveExecutable(String configured) {
        return StringUtils.isBlank(configured) ? DEFAULT_EXECUTABLE : configured;
    }

    /**
     * {@code wslc version} is metadata only: unlike most subcommands it does not start the container
     * VM, which is what makes it usable on the strategy-discovery path.
     * <p>
     * Both streams are discarded, and merged first, so the child can never block on a pipe nobody is
     * draining. {@code Redirect.DISCARD} would say this more directly but is Java 9+, and core main
     * sources compile at {@code release 8}.
     */
    static ProcessBuilder probeCommand(String executable, boolean windows) {
        return new ProcessBuilder(executable, "version")
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.to(new File(windows ? "NUL" : "/dev/null")));
    }

    /**
     * @return true only if the command ran to completion within {@code timeoutMillis} and exited 0.
     *         Every other outcome is a reason not to claim this strategy, and is logged at debug so
     *         a half-installed wslc does not fail silently.
     */
    static boolean probe(ProcessBuilder builder, long timeoutMillis) {
        Process process = null;
        try {
            process = builder.start();
            if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
                log.debug("wslc probe {} did not finish within {}ms", builder.command(), timeoutMillis);
                return false;
            }
            int exitValue = process.exitValue();
            if (exitValue != 0) {
                log.debug("wslc probe {} exited with {}", builder.command(), exitValue);
                return false;
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("wslc probe {} was interrupted", builder.command(), e);
            return false;
        } catch (IOException | RuntimeException e) {
            log.debug("wslc probe {} could not be run", builder.command(), e);
            return false;
        } finally {
            if (process != null) {
                // A no-op once the process has exited, so this needs no isAlive() guard.
                process.destroyForcibly();
            }
        }
    }
}
