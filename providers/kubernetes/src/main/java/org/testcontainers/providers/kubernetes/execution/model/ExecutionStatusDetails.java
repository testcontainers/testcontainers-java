package org.testcontainers.providers.kubernetes.execution.model;

import lombok.Data;

import java.util.List;

@Data
public class ExecutionStatusDetails {

    private List<ExecutionStatusCause> causes;

}
