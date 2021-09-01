package org.testcontainers.providers.kubernetes.execution;

import lombok.Getter;
import org.testcontainers.providers.kubernetes.execution.model.ExecutionStatusCause;
import org.testcontainers.providers.kubernetes.execution.model.ExecutionStatusDetails;
import org.testcontainers.providers.kubernetes.execution.model.ExecutionStatusMessage;

import java.util.Optional;

public class ProcessExitInformation {

    @Getter
    private final int exitCode;
    @Getter
    private final String message;

    private static final ProcessExitInformation SUCCESS = new ProcessExitInformation(0, "");

    public ProcessExitInformation(int exitCode, String message) {
        this.exitCode = exitCode;
        this.message = message;
    }

    public static ProcessExitInformation success() {
        return SUCCESS;
    }

    public static ProcessExitInformation fromStatusMessage(ExecutionStatusMessage message) {
        if("Success".equalsIgnoreCase(message.getStatus())) {
            return success();
        }
        Optional<String> messageString = Optional.ofNullable(message.getMessage());
        Optional<Integer> exitCode = Optional.ofNullable(message.getDetails())
            .map(ExecutionStatusDetails::getCauses)
            .flatMap(causes -> causes.stream()
                .filter(c -> "ExitCode".equalsIgnoreCase(c.getReason()))
                .map(ExecutionStatusCause::getMessage)
                .map(Integer::valueOf)
                .findFirst()
            );
        return new ProcessExitInformation(exitCode.orElse(-1), messageString.orElse("Unknown."));
    }

}
