package org.testcontainers.providers.kubernetes.execution;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.testcontainers.providers.kubernetes.execution.model.ExecutionStatusMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Optional;

@Slf4j
public class KubernetesExecutionErrorListener extends OutputStream {

    private final OutputStream out;
    private final InputStream in;

    @Getter(lazy = true) @Nullable
    private final ProcessExitInformation exitInformation = evaluate();


    @SneakyThrows({ IOException.class })
    public KubernetesExecutionErrorListener(){
        PipedOutputStream pipedOutputStream = new PipedOutputStream();
        out = pipedOutputStream;
        in = new PipedInputStream(pipedOutputStream);
    }


    @Nullable
    private ProcessExitInformation evaluate() {
        try {
            Optional<ExecutionStatusMessage> executionStatusMessage = Optional.ofNullable(
                new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .readValue(in, ExecutionStatusMessage.class)
            );
            return executionStatusMessage.map(ProcessExitInformation::fromStatusMessage).orElse(null);
        }catch (IOException e) {
            log.debug("Could not obtain execution result.", e);
            return null;
        }
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void write(byte[] b) throws IOException {
        out.write(b);
    }


    @Override
    public void close() throws IOException {
        out.close();
        in.close();
        super.close();
    }

    @Override
    public void flush() throws IOException {
        out.flush();
        super.flush();
    }
}
