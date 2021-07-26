package org.testcontainers.utility;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Process execution utility methods.
 */
public class CommandLine {

    private static final Logger LOGGER = getLogger(CommandLine.class);

    /**
     * Run a shell command synchronously.
     *
     * @param command command to run and arguments
     * @return the stdout output of the command
     */
    public static String runShellCommand(String... command) {

        String joinedCommand = String.join(" ", command);
        LOGGER.debug("Executing shell command: `{}`", joinedCommand);

        try {
            ProcessResult result = new ProcessExecutor()
                .command(command)
                .readOutput(true)
                .exitValueNormal()
                .execute();

            return result.outputUTF8().trim();
        } catch (IOException | InterruptedException | TimeoutException | InvalidExitValueException e) {
            throw new ShellCommandException("Exception when executing " + joinedCommand, e);
        }
    }

    /**
     * Check whether an executable exists, either at a specific path (if a full path is given) or
     * on the PATH.
     *
     * @param executable the name of an executable on the PATH or a complete path to an executable that may/may not exist
     * @return  whether the executable exists and is executable
     */
    public static boolean executableExists(String executable) {

        // First check if we've been given the full path already
        File directFile = new File(executable);
        if (directFile.exists() && directFile.canExecute()) {
            return true;
        }

        for (String pathString : getSystemPath()) {
            Path path = Paths.get(pathString);
            if (Files.exists(path.resolve(executable)) && Files.isExecutable(path.resolve(executable))) {
                return true;
            }
        }

        return false;
    }

    @NotNull
    public static String[] getSystemPath() {
        return System.getenv("PATH").split(Pattern.quote(File.pathSeparator));
    }

    private static class ShellCommandException extends RuntimeException {
        public ShellCommandException(String message, Exception e) {
            super(message, e);
        }
    }
}
