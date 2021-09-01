package org.testcontainers.providers.kubernetes.execution.model;

import lombok.Data;

@Data
public class ExecutionStatusCause {

    private String reason;
    private String message;

}
