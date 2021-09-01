package org.testcontainers.providers.kubernetes.execution.model;

import lombok.Data;

@Data
public class ExecutionStatusMessage {

    private String status;
    private String message;
    private String reason;
    private ExecutionStatusDetails details;

}
