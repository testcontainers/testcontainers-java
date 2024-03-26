package org.testcontainers.containers.output;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.Objects;

/**
 * Log consumer that writes logs to a file.
 * <p>
 *     Both STDERR and STDOUT logs are written to the same file.
 *     If the file cannot be created or written to, logs will be written to stdout and stderr instead.
 *     If the file already exists, logs will be appended to it.
 *     If the file becomes not writable after some time, logs will be written to stdout and stderr instead from that
 *     moment and writing to the file will not be retried.
 *
 * <p>Example usage:
 * <pre>
 *     GenericContainer container = new GenericContainer<>("alpine:3");
 *     container.withLogConsumer(new ToFileConsumer(new File("my-log-file.log")));
 *     container.withCommand("echo", "Hello, World!");
 *     container.start();
 * </pre>
 */
@Slf4j
public class ToFileConsumer extends BaseConsumer<ToFileConsumer> {

    private final File logFile;
    private boolean fallback;

    public ToFileConsumer(File logFile) {
        Objects.requireNonNull(logFile, "logFile must not be null");

        this.logFile = logFile.getAbsoluteFile();

        try {
            Files.createFile(logFile.toPath());
            log.info("Created log file " + logFile);
            fallback = false;
        } catch (FileAlreadyExistsException e) {
            // ignore, appending to existing file
            log.info("Log file already exists, appending to " + logFile);
            fallback = false;
        } catch (IOException e) {
            log.error("Could not create log file " + logFile + ", using default stdout and stderr instead", e);
            fallback = true;
        }
    }

    @Override
    public void accept(OutputFrame outputFrame) {
        OutputFrame.OutputType type = outputFrame.getType();

        if (type.equals(OutputFrame.OutputType.END)) {
            return;
        }

        String utf8String = outputFrame.getUtf8String();

        if (fallback) {
            printToSystemOutOrErr(type, utf8String);
            return;
        }

        try (FileWriter fileWriter = new FileWriter(logFile, true)) {
            fileWriter.write(utf8String);
            if (type.equals(OutputFrame.OutputType.STDERR)) {
                fileWriter.flush();
            }
        } catch (IOException e) {
            log.error(
                "Could not write to log file " + logFile + ", from now using default stdout and stderr instead",
                e
            );
            fallback = true;
            printToSystemOutOrErr(type, utf8String);
        }
    }

    private static void printToSystemOutOrErr(OutputFrame.OutputType type, String utf8String) {
        if (type.equals(OutputFrame.OutputType.STDERR)) {
            System.err.print(utf8String);
        } else {
            System.out.print(utf8String);
        }
    }
}
